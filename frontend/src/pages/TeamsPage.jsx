import React, { useEffect, useState } from 'react'
import client from '../api/client'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorAlert from '../components/ErrorAlert'
import './TeamsPage.css'

export default function TeamsPage() {
  const [teams, setTeams] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function load() {
      try {
        const res = await client.get('/api/teams')
        const data = res.data.data
        // Handle both array and paginated response
        const list = Array.isArray(data)
          ? data
          : (data?.content ?? [])
        setTeams(list)
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load teams.')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) return <LoadingSpinner message="Loading teams…" />

  return (
    <div className="teams-page">
      <div className="page-header">
        <h2 className="page-title">Teams</h2>
        <p className="page-subtitle">{teams.length} teams</p>
      </div>

      <ErrorAlert message={error} />

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>ID</th>
              <th>Description</th>
              <th>Contact</th>
            </tr>
          </thead>
          <tbody>
            {teams.map((t, i) => (
              <tr key={t.id ?? i}>
                <td className="cell-name">{t.name}</td>
                <td className="cell-mono">{t.id}</td>
                <td className="cell-secondary">{t.description ?? '—'}</td>
                <td className="cell-secondary">{t.contactEmail ?? t.email ?? '—'}</td>
              </tr>
            ))}
            {teams.length === 0 && !error && (
              <tr>
                <td colSpan={4} className="empty-row">No teams found.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
