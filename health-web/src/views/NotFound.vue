<template>
  <div class="not-found-page">
    <div class="not-found-card">
      <div class="not-found-code">404</div>
      <h2 class="not-found-title">页面不存在</h2>
      <p class="not-found-desc">您访问的页面可能已被移除或地址输入有误</p>
      <el-button type="primary" size="large" @click="$router.push('/dashboard')">
        返回工作台
      </el-button>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F3F6FB;
}
.not-found-card {
  text-align: center;
}
.not-found-code {
  font-size: 120px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea, #3651D5);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}
.not-found-title {
  font-size: 22px;
  color: #303133;
  margin: 0 0 12px;
}
.not-found-desc {
  color: #909399;
  margin: 0 0 28px;
}
</style>
