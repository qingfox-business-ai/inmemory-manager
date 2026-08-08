<template>
  <div class="json-editor-wrapper">
    <div class="json-editor-toolbar">
      <el-button size="small" :icon="MagicStick" :disabled="readonly" @click="formatJson">格式化 JSON</el-button>
      <span v-if="errorMsg" class="json-error">{{ errorMsg }}</span>
    </div>
    <Codemirror
      :model-value="modelValue"
      :extensions="extensions"
      :disabled="readonly"
      :style="{ height: height, fontSize: '13px' }"
      @update:model-value="onUpdate"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'
import { Codemirror } from 'vue-codemirror'
import { json } from '@codemirror/lang-json'

const props = defineProps({
  modelValue: { type: String, default: '' },
  readonly: { type: Boolean, default: false },
  height: { type: String, default: '260px' }
})
const emit = defineEmits(['update:modelValue'])

const extensions = [json()]
const errorMsg = ref('')

const onUpdate = (value) => {
  emit('update:modelValue', value)
  if (!props.readonly) {
    validate(value)
  }
}

const validate = (value) => {
  if (!value || !value.trim()) {
    errorMsg.value = ''
    return true
  }
  try {
    JSON.parse(value)
    errorMsg.value = ''
    return true
  } catch (e) {
    errorMsg.value = 'JSON 格式错误：' + e.message
    return false
  }
}

const formatJson = () => {
  const value = props.modelValue
  if (!value || !value.trim()) return
  try {
    const formatted = JSON.stringify(JSON.parse(value), null, 2)
    emit('update:modelValue', formatted)
    errorMsg.value = ''
  } catch (e) {
    errorMsg.value = '无法格式化：' + e.message
  }
}

if (props.readonly && props.modelValue) {
  try {
    emit('update:modelValue', JSON.stringify(JSON.parse(props.modelValue), null, 2))
  } catch (e) {
    // 保持原值
  }
}

defineExpose({ validate })
</script>

<style scoped>
.json-editor-wrapper {
  width: 100%;
}

.json-editor-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.json-error {
  color: #f56c6c;
  font-size: 12px;
}

.json-editor-wrapper :deep(.cm-editor) {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.json-editor-wrapper :deep(.cm-scroller) {
  font-family: 'Consolas', 'Monaco', monospace;
}
</style>
