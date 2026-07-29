import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    {
      name: 'global-polyfill',
      enforce: 'pre',
      transform(code, id) {
        if (id.includes('sockjs-client')) {
          return code.replace(/typeof global/g, 'typeof window')
        }
      }
    }
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  define: {
    global: 'globalThis'
  },
  server: {
    port: 3000,
    host: true
  }
})
