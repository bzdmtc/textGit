<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/upload'

const props = withDefaults(defineProps<{
  modelValue?: string
  category: string
  accept?: string
  label?: string
}>(), {
  modelValue: '',
  accept: 'image/*',
  label: '上传文件',
})

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

async function handleUpload(options: { file: File; onSuccess?: (response: unknown) => void; onError?: (error: Error) => void }) {
  try {
    const response = await uploadFile(options.file, props.category)
    emit('update:modelValue', response.data.data.url)
    options.onSuccess?.(response.data.data)
  } catch (error) {
    options.onError?.(error as Error)
  }
}

function copyUrl() {
  if (props.modelValue) {
    navigator.clipboard?.writeText(props.modelValue)
    ElMessage.success('地址已复制')
  }
}
</script>

<template>
  <div class="uploader">
    <el-upload
      :show-file-list="false"
      :accept="accept"
      :http-request="handleUpload"
    >
      <el-button>{{ label }}</el-button>
    </el-upload>
    <div v-if="modelValue" class="upload-preview">
      <img v-if="accept.includes('image')" :src="modelValue" alt="preview" />
      <span class="url" @click="copyUrl">{{ modelValue }}</span>
    </div>
  </div>
</template>

<style scoped>
.uploader {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.upload-preview {
  display: flex;
  align-items: center;
  gap: 10px;
}

.upload-preview img {
  width: 120px;
  height: 68px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.url {
  color: #6b7280;
  font-size: 12px;
  word-break: break-all;
  cursor: pointer;
}
</style>
