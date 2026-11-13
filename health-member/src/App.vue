<template>
  <div v-if="isLoginPage" class="app-plain">
    <router-view />
  </div>
  <div v-else class="app-container">
    <router-view />
    <van-tabbar v-model="activeTab" route placeholder>
      <van-tabbar-item icon="home-o" to="/home">首页</van-tabbar-item>
      <van-tabbar-item icon="calendar-o" to="/appointments">预约</van-tabbar-item>
      <van-tabbar-item icon="balance-list-o" to="/assessments">评估</van-tabbar-item>
      <van-tabbar-item icon="other-pay" to="/interventions">干预</van-tabbar-item>
      <van-tabbar-item icon="contact" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isLoginPage = computed(() => route.path === '/login')
const activeTab = ref(0)
</script>

<style>
.app-container { padding-bottom: 50px; }
</style>
