<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button type="primary" :icon="Refresh" @click="handleRefresh">刷新</el-button>
            <el-select v-model="statusFilter" placeholder="状态" style="width: 120px; margin-left: 12px;">
              <el-option label="全部" value="" />
              <el-option label="等待" :value="0" />
              <el-option label="执行中" :value="1" />
              <el-option label="执行成功" :value="2" />
              <el-option label="执行失败" :value="3" />
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
            <el-button type="primary" :icon="Search" @click="handleSearch" style="margin-left: 12px;">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" max-height="600">
        <el-table-column label="任务ID" min-width="200">
          <template #default="{ row }">
            <div class="task-id-cell">
              <span class="task-id-text" :title="row.taskId">{{ row.taskId }}</span>
              <el-tooltip content="复制" placement="top">
                <el-button link size="small" class="copy-btn" @click="copyTaskId(row.taskId)">
                  <el-icon :size="14"><CopyDocument /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="taskMark" label="任务备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.status === 3 && row.errorStack"
              type="danger"
              effect="light"
              class="status-fail-clickable"
              @click="showErrorStack(row)"
            >
              {{ statusLabel(row.status) }}
            </el-tag>
            <el-tag v-else :type="statusTagType(row.status)" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="批次统计" min-width="300">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">等待 {{ row.batchWait || 0 }}</el-tag>
            <el-tag size="small" type="warning" effect="plain" style="margin-left: 4px;">运行 {{ row.batchRun || 0 }}</el-tag>
            <el-tag size="small" type="success" effect="plain" style="margin-left: 4px;">完成 {{ row.batchDone || 0 }}</el-tag>
            <el-tag size="small" type="danger" effect="plain" style="margin-left: 4px;">失败 {{ row.batchFailure || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="消息统计" min-width="300">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">等待 {{ row.queueWait || 0 }}</el-tag>
            <el-tag size="small" type="warning" effect="plain" style="margin-left: 4px;">运行 {{ row.queueRun || 0 }}</el-tag>
            <el-tag size="small" type="success" effect="plain" style="margin-left: 4px;">完成 {{ row.queueDone || 0 }}</el-tag>
            <el-tag size="small" type="danger" effect="plain" style="margin-left: 4px;">失败 {{ row.queueFailure || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数据量(输入/输出)" min-width="140" align="center">
          <template #default="{ row }">
            <span>{{ row.inputCount != null ? row.inputCount : '-' }} / {{ row.outputCount != null ? row.outputCount : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="执行时间" min-width="320">
          <template #default="{ row }">
            <span>{{ row.startTime || '-' }} ~ {{ row.endTime || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showBatchDetail(row)">批次</el-button>
            <el-button size="small" @click="showQueueDetail(row)">队列</el-button>
            <el-button size="small" type="primary" @click="handleMonitor(row)">监控</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end; display: flex;"
        @size-change="handlePageChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <el-dialog v-model="monitorDialogVisible" :title="`监控任务 - ${currentTaskId}`" width="1100px" top="5vh" @close="handleMonitorClose">
      <div v-loading="monitorLoading">
        <!-- 自动刷新工具栏 -->
        <div class="monitor-toolbar">
          <span class="toolbar-label">自动刷新</span>
          <el-select v-model="refreshInterval" size="small" style="width: 100px" @change="onRefreshChange">
            <el-option v-for="opt in refreshOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>

        <!-- 概览区 -->
        <div v-if="monitorData" class="overview-section">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(monitorData.statistics.status)" effect="light">
                {{ statusLabel(monitorData.statistics.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ monitorData.statistics.nowTime }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ monitorData.statistics.startTime }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ monitorData.statistics.endTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="执行时长">{{ formatDuration(monitorData.statistics.duration) }}</el-descriptions-item>
          </el-descriptions>

          <el-row :gutter="16" style="margin-top: 12px;">
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value stat-success">{{ monitorData.statistics.doneCount }}</div>
                <div class="stat-label">成功</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value stat-warning">{{ monitorData.statistics.allCount }}</div>
                <div class="stat-label">等待中</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-value" style="color: #909399;">{{ monitorData.statistics.runningCount }}</div>
                <div class="stat-label">执行中</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div
                class="stat-card"
                :class="{ 'stat-clickable': monitorData.statistics.errorCount > 0 }"
                @click="showTaskErrors"
              >
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
          <el-table-column prop="errorCount" label="异常" width="90" align="center">
            <template #default="{ row }">
              <span
                v-if="row.errorCount > 0"
                class="error-text error-clickable"
                @click="showExecuteErrors(row)"
              >{{ row.errorCount }}</span>
              <span v-else>0</span>
            </template>
          </el-table-column>
        </el-table>

      </div>

      <template #footer>
        <el-button @click="monitorDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" :title="'批次列表 - ' + (currentRow ? currentRow.taskId : '')" width="900px" top="5vh">
      <el-table :data="batchList" v-loading="batchLoading" border style="width: 100%" max-height="500">
        <el-table-column prop="batchId" label="批次ID" min-width="140" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="batchStatusType(row.status)" size="small">{{ batchStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        <el-table-column prop="errorStack" label="错误信息" min-width="200" show-overflow-tooltip />
        <el-table-column prop="startTime" label="开始时间" min-width="170" />
        <el-table-column prop="endTime" label="结束时间" min-width="170" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="queueDialogVisible" :title="'队列列表 - ' + (currentRow ? currentRow.taskId : '')" width="700px" top="5vh">
      <el-table :data="queueList" v-loading="queueLoading" border style="width: 100%" max-height="500">
        <el-table-column prop="id" label="消息ID" min-width="180" />
        <el-table-column label="批次ID" min-width="120">
          <template #default="{ row }">{{ row.attribute && row.attribute.batchId }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="queueStatusType(row.status)" size="small">{{ queueStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="showQueueMessageDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" :title="'消息详情 - ' + (currentMessageId || '')" width="650px" append-to-body>
      <el-table :data="attributeTableData" border style="width: 100%" max-height="500">
        <el-table-column prop="key" label="属性" width="180" />
        <el-table-column prop="value" label="值" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog v-model="errorDialogVisible" :title="errorDialogTitle" width="750px" top="8vh" append-to-body>
      <div v-if="currentErrorList && currentErrorList.length" class="error-list">
        <el-card
          v-for="(err, idx) in currentErrorList"
          :key="idx"
          class="error-card"
          shadow="never"
        >
          <div class="error-header" @click="toggleError(idx)">
            <span class="error-icon">🔴</span>
            <span class="error-title-text">{{ err.substring(0, 100) }}{{ err.length > 100 ? '...' : '' }}</span>
            <span class="error-expand">{{ expandedErrors[idx] ? '收起' : '展开' }}</span>
          </div>
          <div v-if="expandedErrors[idx]" class="error-detail">{{ err }}</div>
        </el-card>
      </div>
      <el-empty v-else description="无异常信息" :image-size="60" />
      <template #footer>
        <el-button @click="errorDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onBeforeUnmount } from 'vue'
import { Refresh, Search, CopyDocument } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskList, getTaskMonitor, getTaskBatches, getStreamMessages } from '@/api'

const statusFilter = ref('')
const taskIdFilter = ref('')
const defaultTimeRange = () => {
  const end = new Date()
  const start = new Date(end.getTime() - 7 * 24 * 60 * 60 * 1000)
  const pad = (n) => String(n).padStart(2, '0')
  const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  return [fmt(start), fmt(end)]
}
const timeRange = ref(defaultTimeRange())
const monitorDialogVisible = ref(false)
const currentTaskId = ref('')
const monitorData = ref(null)
const monitorLoading = ref(false)
const refreshInterval = ref(10000)
const refreshOptions = [
  { label: '无', value: 0 },
  { label: '5秒', value: 5000 },
  { label: '10秒', value: 10000 }
]
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const expandedErrors = reactive({})
const errorDialogVisible = ref(false)
const errorDialogTitle = ref('')
const currentErrorList = ref([])
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
  if (isRunning() && refreshInterval.value > 0) {
    monitorTimer = setInterval(fetchMonitorSilent, refreshInterval.value)
  }
}

const onRefreshChange = () => {
  startMonitorTimer()
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value !== '' && statusFilter.value !== null && statusFilter.value !== undefined) params.status = statusFilter.value
    if (taskIdFilter.value) params.taskId = taskIdFilter.value
    if (timeRange.value && timeRange.value.length === 2) {
      params.startTime = timeRange.value[0]
      params.endTime = timeRange.value[1]
    }
    params.page = currentPage.value
    params.size = pageSize.value
    const res = await getTaskList(params)
    if (res.code === 200 && res.data) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('获取任务数据失败', e)
  } finally {
    loading.value = false
  }
}

// 任务过滤（状态/任务ID/时间范围）已改为后台交互，列表直接使用 tableData

const statusTagType = (status) => {
  const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { 0: '等待', 1: '执行中', 2: '执行成功', 3: '执行失败' }
  return map[status] || status
}

const showErrorStack = (row) => {
  ElMessageBox.alert(row.errorStack || '无错误信息', '执行失败详情', {
    confirmButtonText: '关闭',
    customClass: 'error-stack-alert',
    dangerouslyUseHTMLString: false
  }).catch(() => {})
}

const formatDuration = (ms) => {
  if (!ms || ms <= 0) return '-'
  const totalSec = Math.floor(ms / 1000)
  const days = Math.floor(totalSec / 86400)
  const hours = Math.floor((totalSec % 86400) / 3600)
  const minutes = Math.floor((totalSec % 3600) / 60)
  const seconds = totalSec % 60
  const pad = (n) => String(n).padStart(2, '0')
  return days > 0
    ? `${days}天 ${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
    : `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}

const executeRowClass = ({ row }) => {
  if (row.errorCount > 0) return 'error-row'
  return ''
}

const toggleError = (key) => {
  expandedErrors[key] = !expandedErrors[key]
}

const openErrorDialog = (title, list) => {
  errorDialogTitle.value = title
  currentErrorList.value = list && list.length ? list : []
  Object.keys(expandedErrors).forEach(k => delete expandedErrors[k])
  errorDialogVisible.value = true
}

const showTaskErrors = () => {
  if (monitorData.value && monitorData.value.statistics && monitorData.value.statistics.errorCount > 0) {
    openErrorDialog('任务异常信息', monitorData.value.statistics.errorStack)
  }
}

const showExecuteErrors = (row) => {
  openErrorDialog('执行器异常 - ' + row.executeId, row.errorStack)
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
  errorDialogVisible.value = false
}

const copyTaskId = (taskId) => {
  const textarea = document.createElement('textarea')
  textarea.value = taskId
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  try {
    document.execCommand('copy')
    ElMessage.success('任务ID已复制')
  } catch (e) {
    ElMessage.error('复制失败')
  }
  document.body.removeChild(textarea)
}

const batchDialogVisible = ref(false)
const queueDialogVisible = ref(false)
const currentRow = ref(null)
const batchList = ref([])
const batchLoading = ref(false)
const queueList = ref([])
const queueLoading = ref(false)
const detailDialogVisible = ref(false)
const currentMessageId = ref('')
const currentAttribute = ref(null)

const showBatchDetail = async (row) => {
  currentRow.value = row
  batchDialogVisible.value = true
  batchLoading.value = true
  try {
    const res = await getTaskBatches(row.taskId)
    if (res.code === 200) {
      batchList.value = res.data || []
    }
  } catch (e) {
    console.error('获取批次数据失败', e)
  } finally {
    batchLoading.value = false
  }
}
const showQueueDetail = async (row) => {
  currentRow.value = row
  queueDialogVisible.value = true
  queueLoading.value = true
  try {
    const res = await getStreamMessages({ taskId: row.taskId, page: 1, size: 1000 })
    if (res.code === 200 && res.data) {
      queueList.value = res.data.list || []
    }
  } catch (e) {
    console.error('获取队列数据失败', e)
  } finally {
    queueLoading.value = false
  }
}

const showQueueMessageDetail = (row) => {
  currentMessageId.value = row.id
  currentAttribute.value = row.attribute
  detailDialogVisible.value = true
}

const attributeTableData = computed(() => {
  if (!currentAttribute.value) return []
  return Object.entries(currentAttribute.value).map(([key, value]) => ({
    key,
    value: typeof value === 'object' ? JSON.stringify(value) : String(value)
  }))
})

const batchStatusLabel = (status) => {
  const map = { 0: '等待', 1: '运行', 2: '完成', 3: '失败' }
  return map[status] || status
}
const batchStatusType = (status) => {
  const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}

const queueStatusLabel = (status) => {
  const map = { waiting: '等待', running: '运行中', done: '完成', failed: '失败' }
  return map[status] || status
}
const queueStatusType = (status) => {
  const map = { waiting: 'info', running: 'warning', done: 'success', failed: 'danger' }
  return map[status] || 'info'
}

const batchTableData = computed(() => {
  if (!currentRow.value) return []
  return [
    { label: '等待', value: currentRow.value.batchWait || 0 },
    { label: '运行', value: currentRow.value.batchRun || 0 },
    { label: '完成', value: currentRow.value.batchDone || 0 },
    { label: '失败', value: currentRow.value.batchFailure || 0 },
  ]
})

const queueTableData = computed(() => {
  if (!currentRow.value) return []
  return [
    { label: '等待', value: currentRow.value.queueWait || 0 },
    { label: '运行', value: currentRow.value.queueRun || 0 },
    { label: '完成', value: currentRow.value.queueDone || 0 },
    { label: '失败', value: currentRow.value.queueFailure || 0 },
  ]
})

const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

const handlePageChange = () => {
  fetchData()
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

.task-id-cell {
  display: flex;
  align-items: center;
}

.task-id-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}

.copy-btn {
  margin-left: 4px;
  padding: 0;
  flex-shrink: 0;
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

.monitor-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 16px;
}

.toolbar-label {
  font-size: 13px;
  color: #606266;
}

.stat-card {
  text-align: center;
  padding: 16px 0;
  background: #f5f7fa;
  border-radius: 6px;
}

.stat-clickable {
  cursor: pointer;
  transition: background 0.2s;
}

.stat-clickable:hover {
  background: #fef0f0;
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

.error-clickable {
  cursor: pointer;
  font-weight: bold;
  text-decoration: underline;
}

.error-clickable:hover {
  opacity: 0.75;
}

.status-fail-clickable {
  cursor: pointer;
}

.status-fail-clickable:hover {
  opacity: 0.8;
}

.error-row {
  background-color: #fef0f0 !important;
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
