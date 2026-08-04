<template>
  <div class="stream-view">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>Redis Stream 监控</span>
          <el-button type="primary" :icon="Refresh" @click="fetchStreamInfo" :loading="loading">
            刷新
          </el-button>
        </div>
      </template>

      <el-descriptions :column="2" border class="stream-desc">
        <el-descriptions-item label="Stream名称">{{ streamInfo.streamName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总消息数">{{ streamInfo.totalMessageCount || 0 }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="streamInfo.messages || []" border style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="messageId" label="消息ID" width="200" />
        <el-table-column prop="taskId" label="Task ID" />
        <el-table-column prop="batchId" label="Batch ID" />
        <el-table-column prop="resolverId" label="Resolver ID" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="consumerId" label="消费者ID" width="150" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import axios from 'axios'

const loading = ref(false)
const streamInfo = ref({})

const fetchStreamInfo = async () => {
  loading.value = true
  try {
    const res = await axios.get('/inmemory-manager/inmemory-manager-service/api/stream/info')
    streamInfo.value = res.data.data || {}
  } catch (err) {
    console.error('Failed to fetch stream info:', err)
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  switch (status) {
    case 'PENDING': return 'warning'
    case 'CONSUMING': return 'primary'
    case 'ACKNOWLEDGED': return 'success'
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'PENDING': return '待消费'
    case 'CONSUMING': return '消费中'
    case 'ACKNOWLEDGED': return '已消费'
    default: return status || '-'
  }
}

onMounted(() => {
  fetchStreamInfo()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stream-desc {
  margin-bottom: 20px;
}
</style>
