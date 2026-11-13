import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

export default defineConfig({
  // 子路径部署（如 /triagent/）时必须设置：资源与 API 前缀都由它决定
  base: process.env.VITE_BASE || '/',
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
        // 允许隧道域名访问（Vite 5 默认只接受本机 Host）
    allowedHosts: true,
port: 3000,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
      '/coze-api': {
        target: 'https://api.coze.cn',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/coze-api/, ''),
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('Authorization', `Bearer ${process.env.COZE_PAT || ''}`)
          })
        },
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          echarts: ['echarts'],
        },
      },
    },
  },
  esbuild: {
    drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : [],
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/__tests__/**/*.spec.js'],
  },
})
