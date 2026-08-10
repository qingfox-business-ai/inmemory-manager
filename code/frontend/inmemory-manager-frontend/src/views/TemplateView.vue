<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button type="primary" :icon="Refresh" @click="fetchData">刷新</el-button>
          </div>
          <div class="toolbar-right">
            <el-button type="success" :icon="Plus" @click="handleAdd">新增模板</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" max-height="600" border stripe>
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column prop="resolverId" label="解析器ID" min-width="150" />
        <el-table-column prop="subResolverId" label="子解析器ID" min-width="150" />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="420" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewData(row, 'custom')">查看定制数据</el-button>
            <el-button size="small" @click="viewData(row, 'input')">查看输入数据</el-button>
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该模板？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 查看数据弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="70%" top="5vh">
      <JsonEditor :model-value="dialogContent" readonly height="520px" />
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="65%" top="5vh" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="解析器ID" prop="resolverId">
          <el-input v-model="form.resolverId" placeholder="请输入解析器ID" />
        </el-form-item>
        <el-form-item label="子解析器ID" prop="subResolverId">
          <el-input v-model="form.subResolverId" placeholder="请输入子解析器ID" />
        </el-form-item>
        <el-form-item label="输入ID" prop="inputId">
          <el-input v-model="form.inputId" placeholder="请输入输入统计ID" />
        </el-form-item>
        <el-form-item label="输出ID" prop="outputId">
          <el-input v-model="form.outputId" placeholder="请输入输出统计ID" />
        </el-form-item>
        <el-form-item label="定制数据" prop="customData">
          <JsonEditor v-model="form.customData" height="240px" />
        </el-form-item>
        <el-form-item label="输入数据" prop="inputData">
          <JsonEditor v-model="form.inputData" height="240px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import JsonEditor from '@/components/JsonEditor.vue'
import {
  getTemplateList,
  getTemplateCustomData,
  getTemplateInputData,
  addTemplate,
  updateTemplate,
  deleteTemplate
} from '@/api'

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogContent = ref('')
const formVisible = ref(false)
const formTitle = ref('')
const submitting = ref(false)
const formRef = ref(null)
const isEdit = ref(false)

const defaultForm = () => ({
  id: null,
  templateName: '',
  resolverId: '',
  subResolverId: '',
  inputId: '',
  outputId: '',
  customData: '',
  inputData: ''
})
const form = reactive(defaultForm())

const rules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTemplateList()
    if (res.code === 200 && res.data) {
      tableData.value = res.data
    }
  } catch (e) {
    console.error('获取模板数据失败', e)
  } finally {
    loading.value = false
  }
}

const formatJson = (str) => {
  if (!str) return ''
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch (e) {
    return str
  }
}

const viewData = async (row, type) => {
  try {
    const res = type === 'custom'
      ? await getTemplateCustomData(row.id)
      : await getTemplateInputData(row.id)
    if (res.code === 200) {
      dialogTitle.value = (type === 'custom' ? '定制数据' : '输入数据') + ' - ' + (row.templateName || '')
      dialogContent.value = formatJson(res.data)
      dialogVisible.value = true
    }
  } catch (e) {
    ElMessage.error('加载数据失败')
  }
}

const handleAdd = () => {
  isEdit.value = false
  formTitle.value = '新增模板'
  Object.assign(form, defaultForm())
  formVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  formTitle.value = '编辑模板'
  Object.assign(form, {
    id: row.id,
    templateName: row.templateName || '',
    resolverId: row.resolverId || '',
    subResolverId: row.subResolverId || '',
    inputId: row.inputId || '',
    outputId: row.outputId || '',
    customData: '',
    inputData: ''
  })
  loadDetail(row.id)
  formVisible.value = true
}

const loadDetail = async (id) => {
  try {
    const [c, i] = await Promise.all([getTemplateCustomData(id), getTemplateInputData(id)])
    if (c.code === 200) form.customData = formatJson(c.data || '')
    if (i.code === 200) form.inputData = formatJson(i.data || '')
  } catch (e) {
    console.error('加载详情失败', e)
  }
}

const checkJson = (val, field) => {
  if (!val || !val.trim()) return true
  try {
    JSON.parse(val)
    return true
  } catch (e) {
    ElMessage.error(field + ' 不是合法的JSON格式')
    return false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  if (!checkJson(form.customData, '定制数据')) return
  if (!checkJson(form.inputData, '输入数据')) return
  submitting.value = true
  try {
    const payload = {
      id: form.id,
      templateName: form.templateName,
      resolverId: form.resolverId,
      subResolverId: form.subResolverId,
      inputId: form.inputId,
      outputId: form.outputId,
      customData: form.customData,
      inputData: form.inputData
    }
    const res = isEdit.value ? await updateTemplate(payload) : await addTemplate(payload)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
      formVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    const res = await deleteTemplate(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
}

onMounted(fetchData)
</script>

<style scoped>
.page-container {
  width: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
}
</style>
