<template>
  <div v-if="result" class="result-table-wrapper">
    <el-table
      :data="result.rows"
      border
      stripe
      size="small"
      max-height="400"
      style="width: 100%"
    >
      <el-table-column
        v-for="col in result.columns"
        :key="col"
        :prop="col"
        :label="col"
        min-width="120"
        show-overflow-tooltip
      />
    </el-table>
  </div>
</template>

<script setup>
defineProps({
  result: {
    type: Object,
    default: null
  }
})
</script>

<style scoped>
.result-table-wrapper {
  margin-top: 12px;
}
</style>
