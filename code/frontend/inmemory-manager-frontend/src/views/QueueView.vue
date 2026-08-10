<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button type="primary" :icon="Refresh" @click="handleSearch">刷新</el-button>
            <el-select v-model="statusFilter" placeholder="状态" style="width: 120px; margin-left: 12px;" @change="handleSearch">
              <el-option label="全部" value="" />
              <el-option label="等待" value="waiting" />
              <el-option label="运行中" value="running" />
              <el-option label="完成" value="done" />
              <el-option label="失败" value="failed" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-input v-model="searchKeyword" placeholder="搜索消息ID/任务ID" :prefix-icon="Search" style="width: 240px;" clearable @change="handleSearch" @clear="handleSearch" />
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" max-height="600">
        <el-table-column prop="id" label="消息ID" min-width="180" />
        <el-table-column label="任务ID" min-width="220">
          <template #default="{ row }">{{ row.attribute && row.attribute.taskId }}</template>
        </el-table-column>
        <el-table-column label="批次ID" min-width="120">
          <template #default="{ row }">{{ row.attribute && row.attribute.batchId }}</template>
        </el-table-column>
        <el-table-column label="状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default>
            <span style="color: #c0c4cc">-</span>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getStreamMessages } from '@/api'

const statusFilter = ref('')
const searchKeyword = ref('')
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getStreamMessages({
      page: currentPage.value,
      size: pageSize.value,
      status: statusFilter.value,
      search: searchKeyword.value
    })
    if (res.code === 200 && res.data) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('获取队列消息失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

const handlePageChange = () => {
  fetchData()
}

const statusTagType = (status) => {
  const map = { waiting: 'info', running: 'warning', done: 'success', failed: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { waiting: '等待', running: '运行中', done: '完成', failed: '失败' }
  return map[status] || status
}

onMounted(fetchData)
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
</style>
