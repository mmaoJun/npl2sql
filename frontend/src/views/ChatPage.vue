<template>
  <div class="chat-page">
    <div class="chat-header">
      <DataSourceSelector
        v-model="selectedDsId"
        :data-sources="dsStore.dataSources"
      />
      <el-button text @click="chatStore.clearMessages()">
        <el-icon><Delete /></el-icon>
        清空对话
      </el-button>
    </div>

    <div class="chat-body">
      <div class="chat-messages" ref="messagesRef">
        <div v-if="chatStore.messages.length === 0" class="empty-hint">
          <el-empty description="输入自然语言问题开始查询">
            <template #image>
              <el-icon :size="60" color="#c0c4cc"><ChatDotRound /></el-icon>
            </template>
          </el-empty>
          <div class="example-queries">
            <p class="example-title">示例问题：</p>
            <el-tag
              v-for="q in exampleQueries"
              :key="q"
              class="example-tag"
              @click="fillQuery(q)"
              effect="plain"
              cursor="pointer"
            >
              {{ q }}
            </el-tag>
          </div>
        </div>

        <template v-for="(msg, idx) in chatStore.messages" :key="idx">
          <ChatMessage :message="msg" />
          <ResultTable v-if="msg.result" :result="msg.result" />
        </template>

        <div v-if="chatStore.loading" class="loading-indicator">
          <el-icon class="is-loading"><Loading /></el-icon>
          正在分析并生成 SQL...
        </div>
      </div>
    </div>

    <div class="chat-input-area">
      <ChatInput
        :disabled="chatStore.loading || !selectedDsId"
        @send="handleSend"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useChatStore } from '../stores/chatStore'
import { useDataSourceStore } from '../stores/datasourceStore'
import ChatInput from '../components/ChatInput.vue'
import ChatMessage from '../components/ChatMessage.vue'
import ResultTable from '../components/ResultTable.vue'
import DataSourceSelector from '../components/DataSourceSelector.vue'

const chatStore = useChatStore()
const dsStore = useDataSourceStore()
const messagesRef = ref(null)
const selectedDsId = ref(null)

const exampleQueries = [
  '查询所有用户',
  '统计每个部门的用户数量',
  '查询年龄大于25岁的用户',
  '查询订单金额最高的前10个用户',
  '查询名字包含张的用户'
]

function fillQuery(q) {
  // 触发 ChatInput 填入文本
  handleSend(q)
}

async function handleSend(message) {
  if (!selectedDsId.value) {
    return
  }
  await chatStore.sendMessage(message, selectedDsId.value)
  await nextTick()
  scrollToBottom()
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

watch(() => chatStore.messages.length, scrollToBottom)

onMounted(async () => {
  await dsStore.fetchDataSources()
  if (dsStore.dataSources.length > 0) {
    selectedDsId.value = dsStore.dataSources[0].id
  }
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.chat-body {
  flex: 1;
  overflow: hidden;
}

.chat-messages {
  height: 100%;
  overflow-y: auto;
  padding: 20px;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.example-queries {
  margin-top: 20px;
  text-align: center;
}

.example-title {
  color: #909399;
  font-size: 13px;
  margin-bottom: 10px;
}

.example-tag {
  margin: 4px;
  cursor: pointer;
}

.example-tag:hover {
  color: var(--primary-color);
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  color: #909399;
  font-size: 14px;
}

.chat-input-area {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}
</style>
