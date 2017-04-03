/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.lang.dart.ide.errorTreeView;

import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.Alarm;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerMessages;
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService;
import gnu.trove.THashMap;
import icons.DartIcons;
import org.dartlang.analysis.server.protocol.AnalysisError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@State(
  name = "DartProblemsView",
  storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public class DartProblemsView implements PersistentStateComponent<DartProblemsViewSettings> {
  public static final String TOOLWINDOW_ID = DartBundle.message("dart.analysis.tool.window");

  private static final int TABLE_REFRESH_PERIOD = 300;

  private final Project myProject;
  private final List<DartProblemsViewPanel> myPanels = new ArrayList<>();

  private final Object myLock = new Object(); // use this lock to access myScheduledFilePathToErrors and myAlarm
  private final Map<String, List<AnalysisError>> myScheduledFilePathToErrors = new THashMap<>();
  private final Alarm myAlarm;
  private DartProblemsViewSettings mySettings = new DartProblemsViewSettings();

  private ToolWindow myToolWindow;
  private Icon myCurrentIcon;
  private boolean myAnalysisIsBusy;

  private int myFilesWithErrorsHash;

  private final Runnable myUpdateRunnable = new Runnable() {
    @Override
    public void run() {
      if (ProjectViewPane.ID.equals(ProjectView.getInstance(myProject).getCurrentViewId())) {
        final int hash = DartAnalysisServerService.getInstance(myProject).getFilePathsWithErrorsHash();
        if (myFilesWithErrorsHash != hash) {
          // refresh red squiggles managed by com.jetbrains.lang.dart.projectView.DartNodeDecorator
          myFilesWithErrorsHash = hash;
          ProjectView.getInstance(myProject).refresh();
        }
      }

      final Map<String, List<AnalysisError>> filePathToErrors;
      synchronized (myLock) {
        filePathToErrors = new THashMap<>(myScheduledFilePathToErrors);
        myScheduledFilePathToErrors.clear();
      }

      for (DartProblemsViewPanel panel : myPanels) {
        panel.setErrors(filePathToErrors);
      }
    }
  };

  public DartProblemsView(@NotNull final Project project, @NotNull final ToolWindowManager toolWindowManager) {
    myProject = project;
    myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);
    Disposer.register(project, myAlarm);

    UIUtil.invokeLaterIfNeeded(() -> {
      if (project.isDisposed()) {
        return;
      }
      myPanels.add(new DartProblemsViewPanel(project, new DartProblemsFilter(project, DartProblemsFilter.FileFilterMode.All), mySettings));
      myPanels.add(
        new DartProblemsViewPanel(project, new DartProblemsFilter(project, DartProblemsFilter.FileFilterMode.ContentRoot), mySettings));
      myPanels.add(
        new DartProblemsViewPanel(project, new DartProblemsFilter(project, DartProblemsFilter.FileFilterMode.DartPackage), mySettings));
      myPanels
        .add(new DartProblemsViewPanel(project, new DartProblemsFilter(project, DartProblemsFilter.FileFilterMode.Directory), mySettings));
      myPanels.add(new DartProblemsViewPanel(project, new DartProblemsFilter(project, DartProblemsFilter.FileFilterMode.File), mySettings));

      //myPanel = new DartProblemsViewPanel(project, myFilter, mySettings);

      myToolWindow = toolWindowManager.registerToolWindow(TOOLWINDOW_ID, false, ToolWindowAnchor.BOTTOM, project, true);
      myCurrentIcon = DartIcons.Dart_13;
      updateIcon();

      for (DartProblemsViewPanel panel : myPanels) {
        final Content content = ContentFactory.SERVICE.getInstance().createContent(panel, panel.getDisplayName(), false);
        myToolWindow.getContentManager().addContent(content);

        panel.setToolWindowUpdater(new ToolWindowUpdater() {
          @Override
          public void setIcon(@NotNull Icon icon) {
            myCurrentIcon = icon;
            updateIcon();
          }

          @Override
          public void setHeaderText(@NotNull String headerText) {
            content.setDisplayName(headerText);
          }
        });
      }

      ToolWindowEx toolWindowEx = (ToolWindowEx)myToolWindow;
      toolWindowEx.setTitleActions(new AnalysisServerStatusAction());
      ArrayList<AnAction> gearActions = new ArrayList<>();
      gearActions.add(new AnalysisServerDiagnosticsAction());
      toolWindowEx.setAdditionalGearActions(new DefaultActionGroup(gearActions));


      if (PropertiesComponent.getInstance(project).getBoolean("dart.analysis.tool.window.force.activate", true)) {
        PropertiesComponent.getInstance(project).setValue("dart.analysis.tool.window.force.activate", false, true);
        myToolWindow.activate(null, false);
      }

      Disposer.register(project, () -> myToolWindow.getContentManager().removeAllContents(true));
    });

    project.getMessageBus().connect().subscribe(
      DartAnalysisServerMessages.DART_ANALYSIS_TOPIC, new DartAnalysisServerMessages.DartAnalysisNotifier() {
        @Override
        public void analysisStarted() {
          myAnalysisIsBusy = true;
          UIUtil.invokeLaterIfNeeded(() -> updateIcon());
        }

        @Override
        public void analysisFinished() {
          myAnalysisIsBusy = false;
          UIUtil.invokeLaterIfNeeded(() -> updateIcon());
        }
      }
    );
  }

  void updateIcon() {
    if (myAnalysisIsBusy) {
      myToolWindow.setIcon(ExecutionUtil.getLiveIndicator(myCurrentIcon));
    }
    else {
      myToolWindow.setIcon(myCurrentIcon);
    }
  }

  public static DartProblemsView getInstance(@NotNull final Project project) {
    return ServiceManager.getService(project, DartProblemsView.class);
  }

  @Override
  public DartProblemsViewSettings getState() {
    return mySettings;
  }

  @Override
  public void loadState(DartProblemsViewSettings state) {
    mySettings = state;

    // Update children.
    for(DartProblemsViewPanel panel : myPanels) {
      if (panel != null) {
        panel.updateFromSettings(mySettings);
      }
    }
  }

  public void setCurrentFile(@Nullable final VirtualFile file) {
    for(DartProblemsViewPanel panel : myPanels) {
      if (panel != null && panel.myFilter.setCurrentFile(file) && panel.myFilter.getFileFilterMode() != DartProblemsFilter.FileFilterMode.All) {
          panel.fireGroupingOrFilterChanged();
      }
    }
  }

  public void updateErrorsForFile(@NotNull final String filePath, @NotNull final List<AnalysisError> errors) {
    synchronized (myLock) {
      if (myScheduledFilePathToErrors.isEmpty()) {
        myAlarm.addRequest(myUpdateRunnable, TABLE_REFRESH_PERIOD, ModalityState.NON_MODAL);
      }

      myScheduledFilePathToErrors.put(filePath, errors);
    }
  }

  public void clearAll() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    ProjectView.getInstance(myProject).refresh(); // refresh red waves managed by com.jetbrains.lang.dart.projectView.DartNodeDecorator

    synchronized (myLock) {
      myAlarm.cancelAllRequests();
      myScheduledFilePathToErrors.clear();
    }

    for(DartProblemsViewPanel panel : myPanels) {
      panel.clearAll();
    }
  }

  interface ToolWindowUpdater {
    void setIcon(@NotNull final Icon icon);

    void setHeaderText(@NotNull final String headerText);
  }
}
