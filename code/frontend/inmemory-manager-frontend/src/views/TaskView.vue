<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button type="primary" :icon="Refresh" @click="handleRefresh">刷新</el-button>
            <el-select v-model="statusFilter" placeholder="状态" style="width: 120px; margin-left: 12px;">
              <el-option label="全部" value="" />
              <el-option label="成功" value="success" />
              <el-option label="执行中" value="running" />
              <el-option label="异常" value="failed" />
            </el-select>
            <el-input v-model="taskIdFilter" placeholder="任务ID" style="width: 180px; margin-left: 12px;" clearable />
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              range-separator="~"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
              style="margin-left: 12px;"
            />
          </div>
        </div>
      </template>

      <el-table :data="filteredData" style="width: 100%" max-height="600">
        <el-table-column prop="taskId" label="任务ID" min-width="140" />
        <el-table-column label="批次" width="120" align="center">
          <template #default="{ row }">
            <span>{{ row.completedBatch }} / {{ row.totalBatch }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="180" />
        <el-table-column prop="endTime" label="结束时间" min-width="180" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleMonitor(row)">监控任务</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="monitorDialogVisible" :title="`监控任务 - ${currentTaskId}`" width="700px">
      <pre class="json-content">{{ monitorContent }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getTaskList, getTaskMonitor } from '@/api'

const statusFilter = ref('')
const taskIdFilter = ref('')
const timeRange = ref([])
const monitorDialogVisible = ref(false)
const currentTaskId = ref('')
const monitorContent = ref('')
const tableData = ref([])

const fetchData = async () => {
  try {
    const params = {}
    if (statusFilter.value) params.status = statusFilter.value
    if (taskIdFilter.value) params.taskId = taskIdFilter.value
    if (timeRange.value && timeRange.value.length === 2) {
      params.startTime = timeRange.value[0]
      params.endTime = timeRange.value[1]
    }
    const res = await getTaskList(params)
    if (res.code === 200 && res.data) {
      tableData.value = res.data
    }
  } catch (e) {
    console.error('获取任务数据失败', e)
  }
}

const filteredData = computed(() => {
  return tableData.value.filter(item => {
    const matchStatus = !statusFilter.value || item.status === statusFilter.value
    const matchTaskId = !taskIdFilter.value || item.taskId.toLowerCase().includes(taskIdFilter.value.toLowerCase())
    return matchStatus && matchTaskId
  })
})

const statusTagType = (status) => {
  const map = { success: 'success', running: 'warning', failed: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { success: '✓ 成功', running: '⏳ 执行中', failed: '✗ 异常' }
  return map[status] || status
}

const handleMonitor = async (row) => {
  currentTaskId.value = row.taskId
  try {
    const res = await getTaskMonitor(row.taskId)
    monitorContent.value = JSON.stringify(res.data, null, 2)
  } catch (e) {
    monitorContent.value = JSON.stringify(row.detail || {}, null, 2)
  }
  monitorDialogVisible.value = true
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
  align-items: center;
}

.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
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
