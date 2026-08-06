<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button type="primary" :icon="Refresh" @click="handleRefresh">刷新</el-button>
            <el-select v-model="statusFilter" placeholder="状态" style="width: 120px; margin-left: 12px;">
              <el-option label="全部" value="" />
              <el-option label="在线" value="online" />
              <el-option label="离线" value="offline" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-input v-model="searchKeyword" placeholder="搜索客户端ID/地址" :prefix-icon="Search" style="width: 220px;" clearable />
          </div>
        </div>
      </template>

      <el-table :data="filteredData" v-loading="loading" style="width: 100%" max-height="600">
        <el-table-column type="index" label="排序" width="80" align="center" />
        <el-table-column prop="clientId" label="客户端ID" min-width="140" />
        <el-table-column prop="address" label="客户端地址" min-width="140" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'online' ? 'success' : 'info'" effect="light">
              {{ row.status === 'online' ? '● 在线' : '○ 离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" label="最后一次心跳时间" min-width="180" />
        <el-table-column prop="firstConnect" label="第一次连接时间" min-width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getClientList } from '@/api'

const statusFilter = ref('')
const searchKeyword = ref('')
const tableData = ref([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getClientList()
    if (res.code === 200 && res.data) {
      tableData.value = res.data
    }
  } catch (e) {
    console.error('获取客户端数据失败', e)
  } finally {
    loading.value = false
  }
}

const filteredData = computed(() => {
  return tableData.value.filter(item => {
    const matchStatus = !statusFilter.value || item.status === statusFilter.value
    const matchSearch = !searchKeyword.value ||
      item.clientId.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      item.address.includes(searchKeyword.value)
    return matchStatus && matchSearch
  })
})

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
</style>
