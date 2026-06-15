import React from 'react'
import './Badge.css'

const TYPE_COLORS = {
  SERVICE:  { bg: 'rgba(59,130,246,0.18)',  color: '#60a5fa' },
  DATABASE: { bg: 'rgba(34,197,94,0.18)',   color: '#4ade80' },
  LIBRARY:  { bg: 'rgba(168,85,247,0.18)',  color: '#c084fc' },
  QUEUE:    { bg: 'rgba(249,115,22,0.18)',  color: '#fb923c' },
  GATEWAY:  { bg: 'rgba(20,184,166,0.18)',  color: '#2dd4bf' },
  CACHE:    { bg: 'rgba(236,72,153,0.18)',  color: '#f472b6' },
  FRONTEND: { bg: 'rgba(245,158,11,0.18)',  color: '#fbbf24' },
  WORKER:   { bg: 'rgba(99,102,241,0.18)',  color: '#818cf8' },
}

const STATUS_COLORS = {
  ACTIVE:       { bg: 'rgba(34,197,94,0.18)',   color: '#4ade80' },
  DEPRECATED:   { bg: 'rgba(245,158,11,0.18)',  color: '#fbbf24' },
  RETIRED:      { bg: 'rgba(100,116,139,0.18)', color: '#94a3b8' },
  EXPERIMENTAL: { bg: 'rgba(168,85,247,0.18)',  color: '#c084fc' },
}

export function TypeBadge({ type }) {
  const style = TYPE_COLORS[type] || { bg: 'rgba(100,116,139,0.18)', color: '#94a3b8' }
  return (
    <span
      className="badge"
      style={{ background: style.bg, color: style.color }}
    >
      {type}
    </span>
  )
}

export function StatusBadge({ status }) {
  const style = STATUS_COLORS[status] || { bg: 'rgba(100,116,139,0.18)', color: '#94a3b8' }
  return (
    <span
      className="badge"
      style={{ background: style.bg, color: style.color }}
    >
      {status}
    </span>
  )
}

export const TYPE_COLOR_MAP = TYPE_COLORS
