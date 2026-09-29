<template>
  <div class="chat-page">
    <div class="chat-header">
      <div class="header-left">
        <el-icon :size="18" color="var(--primary)"><Connection /></el-icon>
        <DataSourceSelector
          v-model="selectedDsId"
          :data-sources="dsStore.dataSources"
        />
      </div>
      <button class="clear-btn" @click="chatStore.clearMessages()" title="清空对话">
        <el-icon :size="16"><Delete /></el-icon>
        <span>清空</span>
      </button>
    </div>

    <div class="chat-body">
      <div class="chat-messages" ref="messagesRef">
        <div v-if="chatStore.messages.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              <path d="M8 9h8M8 13h4"/>
            </svg>
          </div>
          <h3 class="empty-title">开始对话</h3>
          <p class="empty-desc">输入自然语言问题，自动生成 SQL 并查询结果</p>
          <div class="example-queries">
            <button
              v-for="q in exampleQueries"
              :key="q"
              class="example-chip"
              @click="fillQuery(q)"
            >
              {{ q }}
            </button>
          </div>
        </div>

        <template v-for="(msg, idx) in chatStore.messages" :key="idx">
          <ChatMessage :message="msg" />
          <ResultTable v-if="msg.result" :result="msg.result" />
        </template>

        <div v-if="chatStore.loading" class="loading-indicator">
          <div class="loading-dots">
            <span></span><span></span><span></span>
          </div>
          <span class="loading-text">正在分析并生成 SQL...</span>
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
  handleSend(q)
}

async function handleSend(message) {
  if (!selectedDsId.value) return
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
  background: var(--bg);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  z-index: 1;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.clear-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition);
}

.clear-btn:hover {
  color: var(--error);
  border-color: var(--error);
  background: var(--error-bg);
}

.chat-body {
  flex: 1;
  overflow: hidden;
}

.chat-messages {
  height: 100%;
  overflow-y: auto;
  padding: 24px 32px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--primary-bg);
  color: var(--primary);
  margin-bottom: 20px;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 28px;
}

.example-queries {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  max-width: 560px;
}

.example-chip {
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: var(--surface);
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition);
  box-shadow: var(--shadow-sm);
}

.example-chip:hover {
  color: var(--primary);
  border-color: var(--primary);
  background: var(--primary-bg);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.12);
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
}

.loading-dots {
  display: flex;
  gap: 4px;
}

.loading-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
  animation: dotPulse 1.4s ease-in-out infinite;
}

.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

.loading-text {
  font-size: 13px;
  color: var(--text-muted);
}
</style>
