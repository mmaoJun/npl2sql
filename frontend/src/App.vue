<template>
  <div class="app-container" :class="{ 'no-sidebar': isAuthPage }">
    <aside v-if="!isAuthPage" class="app-sidebar">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 7h16M4 12h10M4 17h6" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="brand-text">
          <h1>NL2SQL</h1>
          <span>智能查询系统</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link
          to="/chat"
          :class="['nav-item', { active: route.path === '/chat' }]"
        >
          <el-icon :size="18"><ChatDotRound /></el-icon>
          <span>智能对话</span>
        </router-link>
        <router-link
          to="/settings"
          :class="['nav-item', { active: route.path === '/settings' }]"
        >
          <el-icon :size="18"><Setting /></el-icon>
          <span>数据源管理</span>
        </router-link>
      </nav>

      <div class="sidebar-bottom">
        <div class="user-card">
          <div class="user-avatar">
            {{ username.charAt(0).toUpperCase() || '?' }}
          </div>
          <div class="user-info">
            <span class="user-name">{{ username || '未登录' }}</span>
            <button class="logout-btn" @click="logout">退出</button>
          </div>
        </div>
      </div>
    </aside>

    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const isAuthPage = computed(() => route.path === '/login')
const username = ref('')

watch(() => route.path, () => {
  username.value = localStorage.getItem('username') || ''
}, { immediate: true })

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style scoped>
.app-container.no-sidebar .app-main {
  width: 100%;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.brand-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.brand-text h1 {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
  line-height: 1.2;
  letter-spacing: -0.3px;
}

.brand-text span {
  font-size: 11px;
  color: #64748b;
  letter-spacing: 0.5px;
}

.sidebar-nav {
  flex: 1;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  color: #94a3b8;
  text-decoration: none;
  font-size: 13.5px;
  font-weight: 500;
  transition: all 0.15s ease;
  position: relative;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #cbd5e1;
}

.nav-item.active {
  background: rgba(99, 102, 241, 0.15);
  color: #a5b4fc;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  border-radius: 0 3px 3px 0;
  background: #6366f1;
}

.sidebar-bottom {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-name {
  font-size: 13px;
  color: #cbd5e1;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  background: none;
  border: none;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.15s ease;
}

.logout-btn:hover {
  color: #f87171;
  background: rgba(248, 113, 113, 0.1);
}
</style>
