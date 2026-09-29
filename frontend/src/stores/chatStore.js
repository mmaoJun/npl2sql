import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chat } from '../api/endpoints'

export const useChatStore = defineStore('chat', () => {
  const messages = ref([])
  const loading = ref(false)

  function addUserMessage(content) {
    messages.value.push({
      role: 'user',
      content,
      timestamp: Date.now()
    })
  }

  function addSystemMessage(content, sql = null, result = null) {
    messages.value.push({
      role: 'system',
      content,
      sql,
      result,
      timestamp: Date.now()
    })
  }

  async function sendMessage(message, datasourceId) {
    addUserMessage(message)
    loading.value = true

    try {
      const res = await chat({ message, datasourceId })
      const response = res.data

      if (response.success) {
        addSystemMessage(
          `查询成功，共 ${response.rowCount} 条结果（耗时 ${response.executionTimeMs}ms）`,
          response.generatedSql,
          { columns: response.columns, rows: response.rows }
        )
      } else {
        addSystemMessage(response.errorMessage || '查询失败')
      }
    } catch (error) {
      addSystemMessage('请求失败: ' + (error.message || '未知错误'))
    } finally {
      loading.value = false
    }
  }

  function clearMessages() {
    messages.value = []
  }

  return { messages, loading, sendMessage, clearMessages }
})
