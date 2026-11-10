<template>
  <div class="stat-card" :class="gradient">
    <div class="stat-card-inner">
      <div class="stat-info">
        <div class="stat-label">{{ title }}</div>
        <div class="stat-value">
          <span class="count-up">{{ value }}</span>
          <span v-if="unit" class="stat-unit">{{ unit }}</span>
        </div>
        <div class="stat-trend" v-if="trend != null">
          <el-icon :size="12"><component :is="trend > 0 ? 'Top' : 'Bottom'" /></el-icon>
          <span>{{ Math.abs(trend) }}% 较昨日</span>
        </div>
      </div>
      <div class="stat-icon-box">
        <el-icon :size="18"><component :is="icon" /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  value: { type: [Number, String], required: true },
  unit: { type: String, default: '' },
  icon: { type: [String, Object], required: true },
  gradient: { type: String, default: 'gradient-blue' },
  trend: { type: Number, default: null },
})
</script>
