<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="center-page">
    <el-result icon="warning" title="403" sub-title="当前账号没有访问该页面的权限">
      <template #extra>
        <el-button type="primary" @click="router.replace('/admin/dashboard')">返回后台</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.center-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6f8;
}
</style>
