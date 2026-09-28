<template>
  <div class="settings-page">
    <div class="page-header">
      <h3>数据源管理</h3>
      <el-button type="primary" @click="showDialog = true">
        <el-icon><Plus /></el-icon>
        新增数据源
      </el-button>
    </div>

    <el-table :data="dsStore.dataSources" border stripe style="width: 100%">
      <el-table-column prop="name" label="名称" width="150" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="host" label="主机" />
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="databaseName" label="数据库" width="150" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleTest(row.id)" :loading="testingId === row.id">
            测试连接
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增数据源对话框 -->
    <el-dialog v-model="showDialog" title="新增数据源" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如：测试数据库" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="MySQL" value="mysql" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机">
          <el-input v-model="form.host" placeholder="localhost" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="数据库">
          <el-input v-model="form.databaseName" placeholder="数据库名" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useDataSourceStore } from '../stores/datasourceStore'
import { createDataSource, deleteDataSource, testDataSource } from '../api/endpoints'
import { ElMessage, ElMessageBox } from 'element-plus'

const dsStore = useDataSourceStore()
const showDialog = ref(false)
const creating = ref(false)
const testingId = ref(null)

const form = reactive({
  name: '',
  type: 'mysql',
  host: 'localhost',
  port: 3306,
  databaseName: '',
  username: 'root',
  password: ''
})

async function handleCreate() {
  creating.value = true
  try {
    await createDataSource(form)
    ElMessage.success('数据源创建成功')
    showDialog.value = false
    await dsStore.fetchDataSources()
    Object.assign(form, { name: '', type: 'mysql', host: 'localhost', port: 3306, databaseName: '', username: 'root', password: '' })
  } catch (e) {
    // error handled by interceptor
  } finally {
    creating.value = false
  }
}

async function handleTest(id) {
  testingId.value = id
  try {
    const res = await testDataSource(id)
    if (res.data) {
      ElMessage.success('连接成功')
    } else {
      ElMessage.error('连接失败')
    }
  } finally {
    testingId.value = null
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定要删除该数据源吗？', '确认', { type: 'warning' })
  await deleteDataSource(id)
  ElMessage.success('删除成功')
  await dsStore.fetchDataSources()
}

onMounted(() => dsStore.fetchDataSources())
</script>

<style scoped>
.settings-page {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h3 {
  font-size: 18px;
  color: #303133;
}
</style>
