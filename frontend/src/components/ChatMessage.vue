<template>
  <div :class="['message-bubble', message.role === 'user' ? 'message-user' : 'message-system']">
    <div class="message-content">{{ message.content }}</div>

    <div v-if="message.sql" class="sql-display">
      {{ message.sql }}
    </div>

    <div v-if="message.result" class="result-info">
      <el-tag type="success" size="small">
        {{ message.result.columns?.length || 0 }} 列 / {{ message.result.rows?.length || 0 }} 行
      </el-tag>
    </div>
  </div>
</template>

<script setup>
defineProps({
  message: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.message-content {
  margin-bottom: 4px;
}

.result-info {
  margin-top: 8px;
}
</style>
