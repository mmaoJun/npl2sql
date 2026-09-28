<template>
  <div class="app-container">
    <div class="app-sidebar">
      <div class="sidebar-header">
        <h2>NL2SQL</h2>
        <span class="subtitle">智能查询系统</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>智能对话</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <span>数据源管理</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="user-info" v-if="username">
          <el-icon><User /></el-icon>
          <span>{{ username }}</span>
        </div>
        <el-button type="danger" text size="small" @click="logout">
          退出登录
        </el-button>
      </div>
    </div>

    <div class="app-main">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
const username = computed(() => localStorage.getItem('username') || '')

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style scoped>
.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
  text-align: center;
}

.sidebar-header h2 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 4px;
}

.sidebar-header .subtitle {
  font-size: 12px;
  color: #909399;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
}

.sidebar-footer {
  padding: 12px 20px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #606266;
}
</style>
