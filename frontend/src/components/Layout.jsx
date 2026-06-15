import React from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import './Layout.css'

const navItems = [
  { to: '/', label: 'Dashboard', icon: '⬛', end: true },
  { to: '/components', label: 'Components', icon: '⬡' },
  { to: '/teams', label: 'Teams', icon: '◈' },
]

export default function Layout() {
  const { auth, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-icon">⬡</span>
          <span className="brand-text">EDAP</span>
        </div>
        <nav className="sidebar-nav">
          {navItems.map(({ to, label, icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                'nav-item' + (isActive ? ' nav-item--active' : '')
              }
            >
              <span className="nav-icon">{icon}</span>
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <span className="sidebar-version">v1.0.0</span>
        </div>
      </aside>

      <div className="main-area">
        <header className="topbar">
          <div className="topbar-title">Engineering Data Access Platform</div>
          <div className="topbar-right">
            <div className="user-info">
              <span className="user-avatar">
                {auth?.username?.[0]?.toUpperCase() ?? 'U'}
              </span>
              <span className="user-name">{auth?.username}</span>
            </div>
            <button className="btn-logout" onClick={handleLogout}>
              Logout
            </button>
          </div>
        </header>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
