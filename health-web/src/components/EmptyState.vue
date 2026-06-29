<template>
  <div class="empty-state">
    <el-icon :size="64" color="#dcdfe6"><component :is="icon" /></el-icon>
    <p class="empty-description">{{ description }}</p>
    <el-button v-if="actionText" type="primary" @click="$emit('action')">
      {{ actionText }}
    </el-button>
  </div>
</template>

<script setup>
defineProps({
  description: { type: String, default: '暂无数据' },
  icon: { type: String, default: 'Document' },
  actionText: { type: String, default: '' },
})
defineEmits(['action'])
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}
.empty-description {
  color: #909399;
  font-size: 14px;
  margin: 16px 0 8px;
}
</style>
