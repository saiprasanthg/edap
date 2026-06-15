import React, { createContext, useContext, useState, useCallback } from 'react'
import client from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    const stored = localStorage.getItem('edap_auth')
    if (stored) {
      try { return JSON.parse(stored) } catch { return null }
    }
    return null
  })

  const login = useCallback(async (username, password) => {
    const res = await client.post('/api/auth/login', { username, password })
    const { success, data } = res.data
    if (!success) throw new Error('Login failed')
    const authData = {
      token: data.token,
      username: data.username,
      roles: data.roles,
    }
    localStorage.setItem('edap_auth', JSON.stringify(authData))
    setAuth(authData)
    return authData
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('edap_auth')
    setAuth(null)
  }, [])

  return (
    <AuthContext.Provider value={{ auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
