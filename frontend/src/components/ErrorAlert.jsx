import React from 'react'

export default function ErrorAlert({ message }) {
  if (!message) return null
  return (
    <div style={{
      background: 'rgba(239,68,68,0.12)',
      border: '1px solid rgba(239,68,68,0.4)',
      color: '#fca5a5',
      borderRadius: '8px',
      padding: '12px 16px',
      fontSize: '13px',
      marginBottom: '16px',
    }}>
      {message}
    </div>
  )
}
