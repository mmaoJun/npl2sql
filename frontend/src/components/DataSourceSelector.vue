<template>
  <el-select
    :model-value="modelValue"
    placeholder="选择数据源"
    @update:model-value="$emit('update:modelValue', $event)"
    style="width: 100%"
  >
    <el-option
      v-for="ds in dataSources"
      :key="ds.id"
      :label="`${ds.name} (${ds.type})`"
      :value="ds.id"
    />
  </el-select>
</template>

<script setup>
defineProps({
  modelValue: [Number, null],
  dataSources: {
    type: Array,
    default: () => []
  }
})

defineEmits(['update:modelValue'])
</script>
