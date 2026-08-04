<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button type="primary" :icon="Refresh" @click="handleRefresh">刷新</el-button>
            <el-select v-model="statusFilter" placeholder="状态" style="width: 120px; margin-left: 12px;">
              <el-option label="全部" value="" />
              <el-option label="已完成" value="completed" />
              <el-option label="未执行" value="pending" />
              <el-option label="死信" value="dead" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-input v-model="searchKeyword" placeholder="搜索消息ID/任务ID" :prefix-icon="Search" style="width: 220px;" clearable />
          </div>
        </div>
      </template>

      <el-table :data="filteredData" style="width: 100%" max-height="600">
        <el-table-column prop="messageId" label="消息ID" min-width="140" />
        <el-table-column prop="taskId" label="任务ID" min-width="120" />
        <el-table-column prop="batchId" label="批次ID" min-width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewConfig(row)">查看配置</el-button>
            <el-button size="small" type="primary" @click="handleViewData(row)">查看数据</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="configDialogVisible" title="配置信息" width="600px">
      <pre class="json-content">{{ configContent }}</pre>
    </el-dialog>

    <el-dialog v-model="dataDialogVisible" title="数据内容" width="600px">
      <pre class="json-content">{{ dataContent }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getMessageList, getMessageConfig, getMessageData } from '@/api'

const statusFilter = ref('')
const searchKeyword = ref('')
const configDialogVisible = ref(false)
const dataDialogVisible = ref(false)
const configContent = ref('')
const dataContent = ref('')
const tableData = ref([])

const fetchData = async () => {
  try {
    const res = await getMessageList()
    if (res.code === 200 && res.data) {
      tableData.value = res.data
    }
  } catch (e) {
    console.error('获取消息数据失败', e)
  }
}

const filteredData = computed(() => {
  return tableData.value.filter(item => {
    const matchStatus = !statusFilter.value || item.status === statusFilter.value
    const matchSearch = !searchKeyword.value ||
      item.messageId.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      item.taskId.toLowerCase().includes(searchKeyword.value.toLowerCase())
    return matchStatus && matchSearch
  })
})

const statusTagType = (status) => {
  const map = { completed: 'success', pending: 'warning', dead: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { completed: '✓ 已完成', pending: '⏳ 未执行', dead: '✗ 死信' }
  return map[status] || status
}

const handleViewConfig = async (row) => {
  try {
    const res = await getMessageConfig(row.messageId)
    configContent.value = JSON.stringify(res.data, null, 2)
  } catch (e) {
    configContent.value = JSON.stringify(row.config || {}, null, 2)
  }
  configDialogVisible.value = true
}

const handleViewData = async (row) => {
  try {
    const res = await getMessageData(row.messageId)
    dataContent.value = JSON.stringify(res.data, null, 2)
  } catch (e) {
    dataContent.value = JSON.stringify(row.data || {}, null, 2)
  }
  dataDialogVisible.value = true
}

const handleRefresh = () => {
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-container {
  width: 100%;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.json-content {
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 16px;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
  overflow-y: auto;
}
</style>
