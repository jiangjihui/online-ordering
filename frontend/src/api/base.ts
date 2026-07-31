import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
})

api.interceptors.request.use((config) => {
  const auth = sessionStorage.getItem('auth')
  if (auth) {
    config.headers.Authorization = `Basic ${auth}`
  }
  return config
})

api.interceptors.response.use(
  (res) => {
    if (res.data.code === 0) {
      return res.data.data
    }
    return Promise.reject(new Error(res.data.message))
  },
  (err) => {
    if (err.code === 'ERR_NETWORK') {
      ElMessage({ message: '网络连接失败，请检查网络', type: 'error', duration: 3000 })
    } else if (err.code === 'ECONNABORTED') {
      ElMessage({ message: '请求超时，请重试', type: 'error', duration: 3000 })
    } else if (err.response?.status === 400) {
      const msg = err.response.data?.message || '请求参数错误'
      ElMessage({ message: msg, type: 'warning', duration: 3000 })
    } else if (err.response?.status === 401) {
      sessionStorage.removeItem('auth')
      sessionStorage.removeItem('authUser')
      const path = window.location.pathname
      if (path.startsWith('/admin') || path === '/kitchen') {
        window.location.href = '/login'
      }
    } else if (err.response?.status === 404) {
      ElMessage({ message: '请求的资源不存在', type: 'error', duration: 3000 })
    } else if (err.response?.status && err.response.status >= 500) {
      ElMessage({ message: '服务器错误，请稍后重试', type: 'error', duration: 3000 })
    }
    return Promise.reject(err)
  },
)

// 响应拦截器已在运行时把响应解包为 res.data.data（仅保留业务数据），
// 这里覆盖 axios 的类型标注，让 api.get/post/put/delete 返回 Promise<T> 而非 AxiosResponse，
// 避免所有调用点出现 "AxiosResponse -> X" 的类型错误。
export default api as unknown as {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
}
