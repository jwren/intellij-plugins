package org.jetbrains.qodana.staticAnalysis.inspections.runner.log

import com.intellij.diagnostic.ThreadDumper
import com.intellij.diagnostic.dumpCoroutines
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import org.jetbrains.qodana.coroutines.QodanaDispatchers
import org.jetbrains.qodana.staticAnalysis.inspections.runner.TimeCookie
import java.io.IOException
import java.time.Duration
import kotlin.io.path.Path

private val LOG_INTERVAL: Duration = Duration.ofMinutes(10)

private val LOGGER = logger<QodanaThreadDumpLoggingActivity>()

private class QodanaThreadDumpLoggingActivity : QodanaLoggingActivity {
  override suspend fun executeActivity(progressName: String, timeCookie: TimeCookie) {
    while (true) {
      delay(LOG_INTERVAL)
      val prefix = "keep running $progressName ... so far ${timeCookie.formatDuration()}"
      withContext(QodanaDispatchers.IO) {
        dumpThreads(prefix, "too-long-wait-thread-dump-${System.currentTimeMillis()}.txt")
      }
    }
  }
}

private fun dumpThreads(prefix: String, fileName: String) {
  val threadDump = ThreadDumper.dumpThreadsToString()
  val coroutines = dumpCoroutines()

  try {
    val logFile = Path(PathManager.getLogPath(), fileName)
    LOGGER.info("Writing thread dump and coroutines dump to: $logFile")
    logFile.toFile().parentFile?.mkdirs()
    logFile.toFile().writeText("$prefix\n\n$threadDump\n\nCoroutines dump: $coroutines")
  }
  catch (e: IOException) {
    LOGGER.warn("Failed writing thread dump file", e)
  }
}