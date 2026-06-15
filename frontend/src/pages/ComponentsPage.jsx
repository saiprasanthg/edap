import React, { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorAlert from '../components/ErrorAlert'
import { TypeBadge, StatusBadge } from '../components/Badge'
import './ComponentsPage.css'

const PAGE_SIZE = 20

export default function ComponentsPage() {
  const navigate = useNavigate()
  const [components, setComponents] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')

  const load = useCallback(async (p) => {
    setLoading(true)
    setError('')
    try {
      const res = await client.get('/api/components', {
        params: { page: p, size: PAGE_SIZE },
      })
      const data = res.data.data
      setComponents(data.content ?? [])
      setTotal(data.totalElements ?? 0)
      setTotalPages(data.totalPages ?? 1)
      setPage(data.number ?? p)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load components.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load(0) }, [load])

  const filtered = search.trim()
    ? components.filter(
        (c) =>
          c.name?.toLowerCase().includes(search.toLowerCase()) ||
          c.teamName?.toLowerCase().includes(search.toLowerCase()) ||
          c.owner?.toLowerCase().includes(search.toLowerCase())
      )
    : components

  return (
    <div className="components-page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Components</h2>
          <p className="page-subtitle">{total} total components</p>
        </div>
        <input
          className="search-input"
          placeholder="Search by name, team, owner…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <ErrorAlert message={error} />

      {loading ? (
        <LoadingSpinner message="Loading components…" />
      ) : (
        <>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Team</th>
                  <th>Owner</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((c) => (
                  <tr
                    key={c.id}
                    className="clickable-row"
                    onClick={() => navigate(`/components/${c.id}`)}
                  >
                    <td className="cell-name">{c.name}</td>
                    <td><TypeBadge type={c.type} /></td>
                    <td><StatusBadge status={c.status} /></td>
                    <td className="cell-secondary">{c.teamName}</td>
                    <td className="cell-secondary">{c.owner}</td>
                    <td className="cell-secondary">
                      {c.createdAt
                        ? new Date(c.createdAt).toLocaleDateString()
                        : '—'}
                    </td>
                  </tr>
                ))}
                {filtered.length === 0 && (
                  <tr>
                    <td colSpan={6} className="empty-row">
                      {search ? 'No components match your search.' : 'No components found.'}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && !search && (
            <div className="pagination">
              <button
                className="page-btn"
                onClick={() => load(page - 1)}
                disabled={page === 0}
              >
                ← Prev
              </button>
              <span className="page-info">
                Page {page + 1} of {totalPages}
              </span>
              <button
                className="page-btn"
                onClick={() => load(page + 1)}
                disabled={page >= totalPages - 1}
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
