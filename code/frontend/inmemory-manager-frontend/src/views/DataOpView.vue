<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button type="primary" :icon="Refresh" @click="fetchData">刷新</el-button>
          </div>
          <div class="toolbar-right" style="margin-left: auto;">
            <el-button type="success" :icon="Plus" @click="openExecDialog">执行</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" max-height="600" border stripe>
        <el-table-column label="执行内容" min-width="150" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.opType === '1' ? 'primary' : 'warning'">{{ opTypeLabel(row.opType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.status === 3 && row.errorStack"
              :type="statusType(row.status)"
              effect="light"
              class="status-fail-clickable"
              @click="showErrorStack(row)"
            >
              {{ statusLabel(row.status) }}
            </el-tag>
            <el-tag v-else :type="statusType(row.status)" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="180" show-overflow-tooltip />
        <el-table-column label="执行时长" min-width="120" align="center">
          <template #default="{ row }">
            <span>{{ formatElapsed(row.startTime, (row.status === 2 || row.status === 3) ? row.endTime : null) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 执行对话框 -->
    <el-dialog v-model="execDialogVisible" title="执行数据操作" width="60%" top="5vh" @close="resetExecForm">
      <el-form :model="execForm" label-width="110px">
        <el-form-item label="执行内容">
          <el-select v-model="execForm.opType" placeholder="请选择执行内容" style="width: 100%">
            <el-option label="查询缓存数据" value="1" />
            <el-option label="删除rediskey" value="2" />
          </el-select>
        </el-form-item>

        <template v-if="execForm.opType === '1'">
          <el-form-item label="主键">
            <el-input v-model="execForm.key" placeholder="请输入主键" />
          </el-form-item>
          <el-form-item label="HASH主键">
            <el-input v-model="execForm.hashKey" placeholder="请输入HASH主键" />
          </el-form-item>
          <el-form-item label="数据类型">
            <el-input v-model="execForm.valueClass" placeholder="请输入数据类型(valueClass)" />
          </el-form-item>
          <el-form-item label="压缩方式">
            <el-select v-model="execForm.compress" placeholder="请选择压缩方式" style="width: 100%">
              <el-option label="无" value="无" />
              <el-option label="LZ4" value="LZ4" />
            </el-select>
          </el-form-item>
        </template>

        <template v-else-if="execForm.opType === '2'">
          <el-form-item label="keyList">
            <div class="key-list">
              <div v-for="(k, idx) in execForm.keyList" :key="idx" class="key-item">
                <el-input v-model="execForm.keyList[idx]" placeholder="请输入key" />
                <el-button
                  v-if="execForm.keyList.length > 1"
                  :icon="Delete"
                  circle
                  size="small"
                  type="danger"
                  @click="execForm.keyList.splice(idx, 1)"
                />
              </div>
              <el-button :icon="Plus" size="small" @click="execForm.keyList.push('')">添加</el-button>
            </div>
          </el-form-item>
          <el-form-item label="批量数">
            <el-input-number v-model="execForm.batchSize" :min="0" controls-position="right" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="execDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="execSubmitting" @click="handleExecSubmit">确认执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDataOpList, executeDataOp } from '@/api'

const tableData = ref([])
const loading = ref(false)
const execDialogVisible = ref(false)
const execSubmitting = ref(false)

const defaultExecForm = () => ({
  opType: '1',
  key: '',
  hashKey: '',
  valueClass: '',
  compress: '无',
  keyList: [''],
  batchSize: 0
})
const execForm = reactive(defaultExecForm())

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDataOpList()
    if (res.code === 200) {
      tableData.value = res.data || []
    }
  } catch (e) {
    console.error('获取数据操作列表失败', e)
  } finally {
    loading.value = false
  }
}

const opTypeLabel = (opType) => {
  const map = { '1': '查询缓存数据', '2': '删除rediskey' }
  return map[opType] || opType
}

const statusLabel = (status) => {
  const map = { 0: '等待中', 1: '执行中', 2: '执行成功', 3: '执行失败' }
  return map[status] || status
}

const statusType = (status) => {
  const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}

const formatElapsed = (start, end) => {
  if (!start) return '-'
  const s = new Date(String(start).replace(' ', 'T'))
  const e = end ? new Date(String(end).replace(' ', 'T')) : new Date()
  const diff = e.getTime() - s.getTime()
  if (isNaN(diff) || diff < 0) return '-'
  const totalMin = Math.floor(diff / 60000)
  const hours = Math.floor(totalMin / 60)
  const minutes = totalMin % 60
  if (hours > 0) return `${hours}小时${minutes}分`
  return `${minutes}分`
}

const showErrorStack = (row) => {
  ElMessageBox.alert(row.errorStack || '无错误信息', '执行失败详情', {
    confirmButtonText: '关闭'
  }).catch(() => {})
}

const resetExecForm = () => {
  Object.assign(execForm, defaultExecForm())
}

const openExecDialog = () => {
  resetExecForm()
  execDialogVisible.value = true
}

const handleExecSubmit = async () => {
  let opParameter
  if (execForm.opType === '1') {
    opParameter = JSON.stringify({
      key: execForm.key,
      hashKey: execForm.hashKey,
      valueClass: execForm.valueClass,
      compress: execForm.compress
    })
  } else {
    opParameter = JSON.stringify({
      keyList: execForm.keyList.filter(k => k && k.trim() !== ''),
      batchSize: execForm.batchSize
    })
  }
  execSubmitting.value = true
  try {
    const res = await executeDataOp({ opType: execForm.opType, opParameter })
    if (res.code === 200) {
      ElMessage.success('执行成功')
      execDialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '执行失败')
    }
  } catch (e) {
    ElMessage.error('执行失败')
  } finally {
    execSubmitting.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page-container {
  height: 100%;
}
.toolbar {
  display: flex;
  align-items: center;
}
.status-fail-clickable {
  cursor: pointer;
}
.status-fail-clickable:hover {
  opacity: 0.8;
}
.key-list {
  width: 100%;
}
.key-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>
