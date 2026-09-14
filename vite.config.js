import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  server: {
    proxy: {
      '/user': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      '/order': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      '/pay/': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      '/auditor': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      '/admin': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      '/Feedback': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      '/avatar': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      '/chat': {
        target: 'http://localhost:8888',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8888',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})