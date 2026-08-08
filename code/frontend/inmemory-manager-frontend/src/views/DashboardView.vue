<template>
  <div class="dashboard-container" v-loading="loading">
    <!-- 客户端统计 -->
    <div class="section">
      <div class="section-title">客户端统计（实时）</div>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <el-icon class="stat-icon success"><Connection /></el-icon>
              <div class="stat-body">
                <div :class="['stat-value', fetchError ? 'na-text' : 'success-text']">{{ fetchError ? 'N/A' : clientData.connected }}</div>
                <div class="stat-label">已连接客户端</div>
                <div class="stat-unit">台</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <el-icon class="stat-icon danger"><SwitchButton /></el-icon>
              <div class="stat-body">
                <div :class="['stat-value', fetchError ? 'na-text' : 'danger-text']">{{ fetchError ? 'N/A' : clientData.disconnected }}</div>
                <div class="stat-label">连接丢失客户端</div>
                <div class="stat-unit">台</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 队列统计 -->
    <div class="section">
      <div class="section-title">队列统计（实时）</div>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <el-icon class="stat-icon success"><CircleCheck /></el-icon>
              <div class="stat-body">
                <div :class="['stat-value', fetchError ? 'na-text' : 'success-text']">{{ fetchError ? 'N/A' : queueData.completed }}</div>
                <div class="stat-label">执行完成的队列</div>
                <div class="stat-unit">条</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <el-icon class="stat-icon warning"><Clock /></el-icon>
              <div class="stat-body">
                <div :class="['stat-value', fetchError ? 'na-text' : 'warning-text']">{{ fetchError ? 'N/A' : queueData.pending }}</div>
                <div class="stat-label">未执行的队列</div>
                <div class="stat-unit">条</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <el-icon class="stat-icon danger"><WarningFilled /></el-icon>
              <div class="stat-body">
                <div :class="['stat-value', fetchError ? 'na-text' : 'danger-text']">{{ fetchError ? 'N/A' : queueData.deadLetter }}</div>
                <div class="stat-label">死信队列</div>
                <div class="stat-unit">条</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 任务统计 -->
    <div class="section">
      <div class="section-title">
        任务统计
        <span class="stat-period">统计时段：[{{ fetchError ? 'N/A' : taskData.periodStart }} ~ {{ fetchError ? 'N/A' : taskData.periodEnd }}]</span>
      </div>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <el-icon class="stat-icon success"><SuccessFilled /></el-icon>
              <div class="stat-body">
                <div :class="['stat-value', fetchError ? 'na-text' : 'success-text']">{{ fetchError ? 'N/A' : taskData.success }}</div>
                <div class="stat-label">执行成功的任务</div>
                <div class="stat-unit">个</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <el-icon class="stat-icon warning"><Clock /></el-icon>
              <div class="stat-body">
                <div :class="['stat-value', fetchError ? 'na-text' : 'warning-text']">{{ fetchError ? 'N/A' : taskData.waiting }}</div>
                <div class="stat-label">等待中的任务</div>
                <div class="stat-unit">个</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <el-icon class="stat-icon danger"><CircleCloseFilled /></el-icon>
              <div class="stat-body">
                <div :class="['stat-value', fetchError ? 'na-text' : 'danger-text']">{{ fetchError ? 'N/A' : taskData.failed }}</div>
                <div class="stat-label">执行异常的任务</div>
                <div class="stat-unit">个</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboardSummary } from '@/api'

const fetchError = ref(false)
const loading = ref(false)
const clientData = ref({ connected: 0, disconnected: 0 })
const queueData = ref({ completed: 0, pending: 0, deadLetter: 0 })
const taskData = ref({ success: 0, waiting: 0, failed: 0, periodStart: '', periodEnd: '' })

const fetchSummary = async () => {
  loading.value = true
  fetchError.value = false
  try {
    const res = await getDashboardSummary()
    if (res.code === 200 && res.data) {
      clientData.value = res.data.client || clientData.value
      queueData.value = res.data.queue || queueData.value
      taskData.value = res.data.task || taskData.value
    } else {
      fetchError.value = true
    }
  } catch (e) {
    fetchError.value = true
    console.error('获取首页数据失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchSummary()
})
</script>

<style scoped>
.dashboard-container {
  width: 100%;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.stat-period {
  font-size: 13px;
  font-weight: normal;
  color: #909399;
  margin-left: 12px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 48px;
}

.stat-icon.success {
  color: #67c23a;
}

.stat-icon.danger {
  color: #f56c6c;
}

.stat-icon.warning {
  color: #e6a23c;
}

.stat-body {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  line-height: 1.2;
}

.success-text {
  color: #67c23a;
}

.danger-text {
  color: #f56c6c;
}

.warning-text {
  color: #e6a23c;
}

.na-text {
  color: #f56c6c;
  font-size: 28px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-unit {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}
</style>
