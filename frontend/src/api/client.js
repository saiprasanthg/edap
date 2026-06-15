import axios from 'axios'

const client = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 15000,
})

// Attach JWT from localStorage on every request
client.interceptors.request.use((config) => {
  const stored = localStorage.getItem('edap_auth')
  if (stored) {
    try {
      const { token } = JSON.parse(stored)
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    } catch {
      // ignore parse errors
    }
  }
  return config
})

// On 401, clear auth and redirect to login
client.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('edap_auth')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default client
