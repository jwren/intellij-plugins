<script setup lang="ts">
import {computed} from "vue";

interface FieldSlotPropText {
  value: object | (() => object),
  required: boolean | (() => boolean),
  class: string | string[],
  style: string | object,
}

defineOptions(
  {
    inheritAttrs: false,
  }
)

defineSlots<{
  default?: (props: { field: FieldSlotPropText }) => any
  abc?: (props: { fieldAbc: FieldSlotPropText }) => any
  header?: (props: { fieldHeader: FieldSlotPropText }) => any
}>()

defineProps<{ prop1?: string, prop2?: string }>()

const fieldData = computed<FieldSlotPropText>(() => {
  return {
    value: '',
    required: false,
    class: '',
    style: '',
  }
})
</script>

<template>
  <slot v-bind:field="fieldData"></slot>
</template>
