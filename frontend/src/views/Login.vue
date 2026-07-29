<template>
  <div class="login-page">
    <div class="login-card">
      <h2>餐厅管理系统</h2>
      <el-form @submit.prevent="login">
        <el-form-item label="用户名">
          <el-input v-model="username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">登录</el-button>
      </el-form>
      <p v-if="error" class="login-error">{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function login() {
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const token = btoa(`${username.value}:${password.value}`)
    const res = await fetch(`${import.meta.env.VITE_API_BASE_URL}/orders/stats`, {
      headers: { Authorization: `Basic ${token}` },
    })
    if (res.ok) {
      sessionStorage.setItem('auth', token)
      sessionStorage.setItem('authUser', username.value)
      router.push('/admin')
    } else if (res.status === 401) {
      error.value = '用户名或密码错误'
    } else {
      error.value = '登录失败'
    }
  } catch {
    error.value = '网络连接失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--color-bg);
}
.login-card {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  width: 360px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}
.login-card h2 { text-align: center; margin-bottom: var(--spacing-lg); }
.login-error { color: var(--color-danger); text-align: center; margin-top: var(--spacing-md); font-size: var(--font-size-sm); }
</style>
