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
              <el-option label="等待中" value="waiting" />
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
            <el-button type="primary" :icon="Search" @click="fetchData" style="margin-left: 12px;">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" max-height="600">
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

    <el-dialog v-model="monitorDialogVisible" :title="`监控任务 - ${currentTaskId}`" width="1100px" top="5vh" @close="handleMonitorClose">
      <div v-loading="monitorLoading">
        <!-- 概览区 -->
        <div v-if="monitorData" class="overview-section">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="状态">
              <el-tag :type="monitorData.statistics.status === 1 ? 'warning' : 'success'" effect="light">
                {{ monitorData.statistics.status === 1 ? '进行中' : '已完成' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="数据库时间">{{ monitorData.statistics.nowTime }}</el-descriptions-item>
            <el-descriptions-item label="异常批次">{{ monitorData.statistics.errorCount }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ monitorData.statistics.startTime }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ monitorData.statistics.endTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label=""></el-descriptions-item>
          </el-descriptions>

          <el-row :gutter="16" style="margin-top: 12px;">
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value">{{ monitorData.statistics.allCount }}</div>
                <div class="stat-label">总批次</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value stat-success">{{ monitorData.statistics.doneCount }}</div>
                <div class="stat-label">已完成</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value stat-warning">{{ monitorData.statistics.runningCount }}</div>
                <div class="stat-label">进行中</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value stat-danger">{{ monitorData.statistics.errorCount }}</div>
                <div class="stat-label">异常</div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 执行器统计 -->
        <div v-if="monitorData && monitorData.executes && monitorData.executes.length" class="section-title">
          执行器统计
        </div>
        <el-table
          v-if="monitorData && monitorData.executes && monitorData.executes.length"
          :data="monitorData.executes"
          style="width: 100%"
          size="small"
          max-height="300"
          :row-class-name="executeRowClass"
        >
          <el-table-column prop="executeId" label="执行器ID" min-width="200" />
          <el-table-column prop="executeType" label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.executeType === 'RESOLVER' ? 'primary' : 'info'">
                {{ row.executeType === 'RESOLVER' ? '解析器' : '执行器' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="runCount" label="执行次数" width="90" align="center" />
          <el-table-column prop="inCount" label="输入总数" width="90" align="center" />
          <el-table-column prop="outCount" label="输出总数" width="90" align="center" />
          <el-table-column prop="duration" label="总耗时(ms)" width="110" align="center" />
          <el-table-column prop="avgIn" label="平均输入" width="90" align="center" />
          <el-table-column prop="avgOut" label="平均输出" width="90" align="center" />
          <el-table-column prop="outPerSecond" label="输出/秒" width="90" align="center" />
          <el-table-column prop="updateTime" label="更新时间" width="160" align="center" />
          <el-table-column prop="errorCount" label="异常" width="80" align="center">
            <template #default="{ row }">
              <el-badge v-if="row.errorCount > 0" :value="row.errorCount" type="danger">
                <span class="error-text">{{ row.errorCount }}</span>
              </el-badge>
              <span v-else>0</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.errorCount > 0"
                size="small"
                type="danger"
                link
                @click="viewExecuteErrors(row)"
              >查看异常</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 异常信息区 -->
        <div v-if="monitorData" class="section-title">
          异常信息
        </div>
        <el-tabs v-if="monitorData" v-model="errorTab" class="error-tabs">
          <el-tab-pane label="批次异常" name="batch">
            <div v-if="monitorData.statistics.errorStack && monitorData.statistics.errorStack.length" class="error-list">
              <el-card
                v-for="(err, idx) in monitorData.statistics.errorStack"
                :key="'batch-' + idx"
                class="error-card"
                shadow="never"
              >
                <div class="error-header" @click="toggleError('batch-' + idx)">
                  <span class="error-icon">🔴</span>
                  <span class="error-title-text">{{ err.substring(0, 100) }}{{ err.length > 100 ? '...' : '' }}</span>
                  <span class="error-expand">{{ expandedErrors['batch-' + idx] ? '收起' : '展开' }}</span>
                </div>
                <div v-if="expandedErrors['batch-' + idx]" class="error-detail">{{ err }}</div>
              </el-card>
            </div>
            <el-empty v-else description="无批次异常" :image-size="60" />
          </el-tab-pane>

          <el-tab-pane label="执行器异常" name="execute">
            <div v-if="executeErrors.length" class="error-list">
              <el-card
                v-for="(err, idx) in executeErrors"
                :key="'exec-' + idx"
                class="error-card"
                shadow="never"
              >
                <div class="error-header" @click="toggleError('exec-' + idx)">
                  <span class="error-icon">🔴</span>
                  <span class="error-title-text">{{ err.substring(0, 100) }}{{ err.length > 100 ? '...' : '' }}</span>
                  <span class="error-expand">{{ expandedErrors['exec-' + idx] ? '收起' : '展开' }}</span>
                </div>
                <div v-if="expandedErrors['exec-' + idx]" class="error-detail">{{ err }}</div>
              </el-card>
            </div>
            <el-empty v-else description="无执行器异常" :image-size="60" />
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <el-button @click="monitorDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onBeforeUnmount } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getTaskList, getTaskMonitor } from '@/api'

const MONITOR_POLL_INTERVAL = 10000

const statusFilter = ref('')
const taskIdFilter = ref('')
const defaultTimeRange = () => {
  const end = new Date()
  const start = new Date(end.getTime() - 2 * 24 * 60 * 60 * 1000)
  const pad = (n) => String(n).padStart(2, '0')
  const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  return [fmt(start), fmt(end)]
}
const timeRange = ref(defaultTimeRange())
const monitorDialogVisible = ref(false)
const currentTaskId = ref('')
const monitorData = ref(null)
const monitorLoading = ref(false)
const tableData = ref([])
const loading = ref(false)
const errorTab = ref('batch')
const expandedErrors = reactive({})
let monitorTimer = null

const clearMonitorTimer = () => {
  if (monitorTimer) {
    clearInterval(monitorTimer)
    monitorTimer = null
  }
}

const isRunning = () => {
  return monitorData.value && monitorData.value.statistics && monitorData.value.statistics.status === 1
}

const fetchMonitorSilent = async () => {
  if (!currentTaskId.value) return
  try {
    const res = await getTaskMonitor(currentTaskId.value)
    if (res.code === 200 && res.data) {
      monitorData.value = res.data
      if (!isRunning()) {
        clearMonitorTimer()
      }
    }
  } catch (e) {
    console.error('监控数据轮询失败', e)
  }
}

const startMonitorTimer = () => {
  clearMonitorTimer()
  if (isRunning()) {
    monitorTimer = setInterval(fetchMonitorSilent, MONITOR_POLL_INTERVAL)
  }
}

const fetchData = async () => {
  loading.value = true
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
  } finally {
    loading.value = false
  }
}

// 任务过滤（状态/任务ID/时间范围）已改为后台交互，列表直接使用 tableData

const statusTagType = (status) => {
  const map = { success: 'success', running: 'warning', waiting: 'info', failed: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { success: '成功', running: '执行中', waiting: '等待中', failed: '异常' }
  return map[status] || status
}

const executeErrors = computed(() => {
  if (!monitorData.value || !monitorData.value.executes) return []
  const errors = []
  for (const exec of monitorData.value.executes) {
    if (exec.errorStack && exec.errorStack.length) {
      errors.push(...exec.errorStack)
    }
  }
  return errors
})

const executeRowClass = ({ row }) => {
  if (row.errorCount > 0) return 'error-row'
  return ''
}

const toggleError = (key) => {
  expandedErrors[key] = !expandedErrors[key]
}

const viewExecuteErrors = (row) => {
  errorTab.value = 'execute'
}

const handleMonitor = async (row) => {
  clearMonitorTimer()
  currentTaskId.value = row.taskId
  monitorDialogVisible.value = true
  monitorLoading.value = true
  monitorData.value = null
  Object.keys(expandedErrors).forEach(k => delete expandedErrors[k])
  try {
    const res = await getTaskMonitor(row.taskId)
    if (res.code === 200) {
      monitorData.value = res.data
      startMonitorTimer()
    }
  } catch (e) {
    console.error('获取监控数据失败', e)
  } finally {
    monitorLoading.value = false
  }
}

const handleMonitorClose = () => {
  clearMonitorTimer()
}

const handleRefresh = () => {
  fetchData()
}

onMounted(() => {
  fetchData()
})

onBeforeUnmount(() => {
  clearMonitorTimer()
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

.overview-section {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 16px 0;
  background: #f5f7fa;
  border-radius: 6px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-success { color: #67c23a; }
.stat-warning { color: #e6a23c; }
.stat-danger { color: #f56c6c; }

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.section-title {
  font-size: 15px;
  font-weight: bold;
  color: #303133;
  margin: 20px 0 12px 0;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.error-text {
  color: #f56c6c;
}

.error-row {
  background-color: #fef0f0 !important;
}

.error-tabs {
  height: 320px;
  overflow-y: auto;
}

.error-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.error-card {
  border-left: 3px solid #f56c6c !important;
}

.error-header {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 8px;
}

.error-icon {
  font-size: 12px;
}

.error-title-text {
  flex: 1;
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.error-expand {
  font-size: 12px;
  color: #409eff;
  white-space: nowrap;
}

.error-detail {
  margin-top: 8px;
  padding: 12px;
  background: #fdf6f6;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow-y: auto;
}
</style>
