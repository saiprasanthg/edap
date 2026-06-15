import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorAlert from '../components/ErrorAlert'
import { TypeBadge } from '../components/Badge'
import './DashboardPage.css'

const TYPE_ORDER = ['SERVICE','DATABASE','LIBRARY','QUEUE','GATEWAY','CACHE','FRONTEND','WORKER']

export default function DashboardPage() {
  const navigate = useNavigate()
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function load() {
      try {
        const [compRes, teamRes] = await Promise.all([
          client.get('/api/components', { params: { size: 1000 } }),
          client.get('/api/teams'),
        ])

        const components = compRes.data.data?.content ?? []
        const teams = teamRes.data.data ?? []

        const typeCounts = {}
        const statusCounts = {}
        components.forEach((c) => {
          typeCounts[c.type] = (typeCounts[c.type] || 0) + 1
          statusCounts[c.status] = (statusCounts[c.status] || 0) + 1
        })

        setStats({
          total: compRes.data.data?.totalElements ?? components.length,
          teamCount: Array.isArray(teams) ? teams.length : (teams.totalElements ?? 0),
          typeCounts,
          statusCounts,
          recentComponents: components.slice(0, 5),
        })
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load dashboard data.')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) return <LoadingSpinner message="Loading dashboard…" />
  if (error) return <ErrorAlert message={error} />

  const { total, teamCount, typeCounts, statusCounts, recentComponents } = stats

  return (
    <div className="dashboard">
      <div className="page-header">
        <h2 className="page-title">Dashboard</h2>
        <p className="page-subtitle">Platform overview and component statistics</p>
      </div>

      {/* Stat cards */}
      <div className="stat-grid">
        <StatCard
          label="Total Components"
          value={total}
          icon="⬡"
          accent="var(--accent)"
        />
        <StatCard
          label="Teams"
          value={teamCount}
          icon="◈"
          accent="var(--purple)"
        />
        <StatCard
          label="Active"
          value={statusCounts['ACTIVE'] ?? 0}
          icon="●"
          accent="var(--success)"
        />
        <StatCard
          label="Deprecated"
          value={statusCounts['DEPRECATED'] ?? 0}
          icon="⚠"
          accent="var(--warning)"
        />
      </div>

      {/* Type breakdown */}
      <div className="section-row">
        <div className="card flex-1">
          <h3 className="card-title">Components by Type</h3>
          <div className="type-breakdown">
            {TYPE_ORDER.map((type) => {
              const count = typeCounts[type] || 0
              const pct = total > 0 ? (count / total) * 100 : 0
              return (
                <div key={type} className="type-row">
                  <TypeBadge type={type} />
                  <div className="type-bar-wrap">
                    <div className="type-bar" style={{ width: `${pct}%` }} />
                  </div>
                  <span className="type-count">{count}</span>
                </div>
              )
            })}
          </div>
        </div>

        <div className="card flex-1">
          <h3 className="card-title">Recent Components</h3>
          <table className="mini-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Team</th>
              </tr>
            </thead>
            <tbody>
              {recentComponents.map((c) => (
                <tr
                  key={c.id}
                  className="clickable-row"
                  onClick={() => navigate(`/components/${c.id}`)}
                >
                  <td>{c.name}</td>
                  <td><TypeBadge type={c.type} /></td>
                  <td style={{ color: 'var(--text-secondary)' }}>{c.teamName}</td>
                </tr>
              ))}
              {recentComponents.length === 0 && (
                <tr><td colSpan={3} style={{ color: 'var(--text-muted)', textAlign: 'center' }}>No components</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

function StatCard({ label, value, icon, accent }) {
  return (
    <div className="stat-card" style={{ borderTopColor: accent }}>
      <div className="stat-icon" style={{ color: accent }}>{icon}</div>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  )
}
