<template>
  <div class="chat-input-wrapper">
    <el-input
      v-model="inputText"
      type="textarea"
      :rows="2"
      placeholder="请输入你的问题，例如：查询年龄大于25岁的用户"
      :disabled="disabled"
      @keydown.enter.exact.prevent="handleSend"
      resize="none"
    />
    <el-button
      type="primary"
      :loading="disabled"
      :disabled="!inputText.trim()"
      @click="handleSend"
      class="send-btn"
    >
      <el-icon><Promotion /></el-icon>
      发送
    </el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  disabled: Boolean
})

const emit = defineEmits(['send'])
const inputText = ref('')

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  inputText.value = ''
}
</script>

<style scoped>
.chat-input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.chat-input-wrapper :deep(.el-textarea__inner) {
  border-radius: 8px;
  font-size: 14px;
}

.send-btn {
  height: 54px;
  min-width: 80px;
  border-radius: 8px;
}
</style>
