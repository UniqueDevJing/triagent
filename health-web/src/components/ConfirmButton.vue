<template>
  <el-popconfirm
    :title="title"
    :confirm-button-text="confirmText"
    :cancel-button-text="cancelText"
    confirm-button-type="danger"
    @confirm="$emit('confirm')"
  >
    <template #reference>
      <el-button v-bind="$attrs" :type="type" :size="size">
        <slot />
      </el-button>
    </template>
  </el-popconfirm>
</template>

<script setup>
defineProps({
  title: { type: String, default: '确定要删除吗？此操作不可恢复。' },
  confirmText: { type: String, default: '确定删除' },
  cancelText: { type: String, default: '取消' },
  type: { type: String, default: 'danger' },
  size: { type: String, default: 'small' },
})
defineEmits(['confirm'])
</script>
