import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端部署在 /v2-web/，后端在 /v2/api
export default defineConfig({
  base: '/v2-web/',
  plugins: [vue()],
  server: {
    port: 5192,
    proxy: {
      // 只代理真正的后端接口；/v2-web/ 是前端 base，不能被 /v2 前缀误伤
      '/v2/api': { target: process.env.V2_BACKEND || 'http://127.0.0.1:18093', changeOrigin: true }
    }
  }
})
