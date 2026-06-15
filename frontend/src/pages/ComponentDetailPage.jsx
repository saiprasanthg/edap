import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import client from '../api/client'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorAlert from '../components/ErrorAlert'
import { TypeBadge, StatusBadge } from '../components/Badge'
import DependencyGraph from '../components/DependencyGraph'
import './ComponentDetailPage.css'

export default function ComponentDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [component, setComponent] = useState(null)
  const [graph, setGraph] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function load() {
      setLoading(true)
      setError('')
      try {
        const [compRes, graphRes] = await Promise.all([
          client.get(`/api/components/${id}`),
          client.get(`/api/dependencies/graph/${id}`).catch(() => null),
        ])
        setComponent(compRes.data.data ?? compRes.data)
        setGraph(graphRes?.data?.data ?? null)
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load component.')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  if (loading) return <LoadingSpinner message="Loading component…" />
  if (error) return (
    <div>
      <button className="back-btn" onClick={() => navigate('/components')}>← Back</button>
      <ErrorAlert message={error} />
    </div>
  )
  if (!component) return null

  const c = component

  return (
    <div className="detail-page">
      <button className="back-btn" onClick={() => navigate('/components')}>
        ← Back to Components
      </button>

      <div className="detail-header">
        <div>
          <h2 className="detail-name">{c.name}</h2>
          {c.description && (
            <p className="detail-desc">{c.description}</p>
          )}
        </div>
        <div className="detail-badges">
          <TypeBadge type={c.type} />
          <StatusBadge status={c.status} />
        </div>
      </div>

      <div className="detail-grid">
        {/* Meta fields */}
        <div className="card detail-meta">
          <h3 className="card-title">Details</h3>
          <dl className="meta-list">
            <MetaRow label="ID" value={c.id} mono />
            <MetaRow label="Team" value={c.teamName} />
            <MetaRow label="Owner" value={c.owner} />
            <MetaRow label="Type" value={<TypeBadge type={c.type} />} />
            <MetaRow label="Status" value={<StatusBadge status={c.status} />} />
            <MetaRow
              label="Repository"
              value={
                c.repositoryUrl ? (
                  <a
                    href={c.repositoryUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="repo-link"
                  >
                    {c.repositoryUrl}
                  </a>
                ) : '—'
              }
            />
            <MetaRow
              label="Created"
              value={c.createdAt ? new Date(c.createdAt).toLocaleString() : '—'}
            />
          </dl>
        </div>

        {/* Dependency graph */}
        <div className="card detail-graph">
          <h3 className="card-title">
            Dependency Graph
            {graph?.nodes && (
              <span className="graph-meta">
                {graph.nodes.length} nodes · {graph.edges?.length ?? 0} edges
              </span>
            )}
          </h3>
          {graph ? (
            <DependencyGraph graph={graph} />
          ) : (
            <div className="no-graph">No dependency graph available.</div>
          )}
        </div>
      </div>
    </div>
  )
}

function MetaRow({ label, value, mono }) {
  return (
    <div className="meta-row">
      <dt className="meta-label">{label}</dt>
      <dd className={`meta-value${mono ? ' mono' : ''}`}>
        {value ?? '—'}
      </dd>
    </div>
  )
}
