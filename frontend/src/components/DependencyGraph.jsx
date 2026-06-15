import React, { useMemo } from 'react'

const TYPE_FILL = {
  SERVICE:  '#3b82f6',
  DATABASE: '#22c55e',
  LIBRARY:  '#a855f7',
  QUEUE:    '#f97316',
  GATEWAY:  '#14b8a6',
  CACHE:    '#ec4899',
  FRONTEND: '#f59e0b',
  WORKER:   '#6366f1',
}

const NODE_R = 28
const WIDTH = 700
const HEIGHT = 500
const CX = WIDTH / 2
const CY = HEIGHT / 2

function layoutNodes(nodes) {
  if (!nodes || nodes.length === 0) return []
  if (nodes.length === 1) {
    return [{ ...nodes[0], x: CX, y: CY }]
  }
  // Put first node (root) in center, rest in a circle around it
  const others = nodes.slice(1)
  const radius = Math.min(CX, CY) - 70
  const angleStep = (2 * Math.PI) / others.length

  return [
    { ...nodes[0], x: CX, y: CY },
    ...others.map((n, i) => ({
      ...n,
      x: CX + radius * Math.cos(i * angleStep - Math.PI / 2),
      y: CY + radius * Math.sin(i * angleStep - Math.PI / 2),
    })),
  ]
}

function truncate(text, max = 12) {
  if (!text) return ''
  return text.length > max ? text.slice(0, max - 1) + '…' : text
}

export default function DependencyGraph({ graph }) {
  const { nodes: rawNodes = [], edges = [], rootId } = graph || {}

  const nodes = useMemo(() => {
    // Root node first
    const sorted = [...rawNodes].sort((a, b) => {
      if (a.componentId === rootId) return -1
      if (b.componentId === rootId) return 1
      return 0
    })
    return layoutNodes(sorted)
  }, [rawNodes, rootId])

  const nodeMap = useMemo(() => {
    const m = {}
    nodes.forEach((n) => { m[n.componentId] = n })
    return m
  }, [nodes])

  if (!rawNodes.length) {
    return (
      <div style={{ color: 'var(--text-muted)', padding: '24px', textAlign: 'center', fontSize: '13px' }}>
        No dependency data available.
      </div>
    )
  }

  return (
    <svg
      viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
      width="100%"
      style={{ background: 'var(--bg-primary)', borderRadius: '10px', display: 'block' }}
      aria-label="Dependency graph"
    >
      <defs>
        <marker
          id="arrow"
          markerWidth="8"
          markerHeight="8"
          refX="6"
          refY="3"
          orient="auto"
        >
          <path d="M0,0 L0,6 L8,3 z" fill="rgba(148,163,184,0.7)" />
        </marker>
      </defs>

      {/* Edges */}
      {edges.map((edge, i) => {
        const src = nodeMap[edge.sourceId]
        const tgt = nodeMap[edge.targetId]
        if (!src || !tgt) return null

        // Shorten line so it doesn't overlap nodes
        const dx = tgt.x - src.x
        const dy = tgt.y - src.y
        const len = Math.sqrt(dx * dx + dy * dy) || 1
        const ux = dx / len
        const uy = dy / len
        const gap = NODE_R + 4

        return (
          <line
            key={i}
            x1={src.x + ux * gap}
            y1={src.y + uy * gap}
            x2={tgt.x - ux * gap}
            y2={tgt.y - uy * gap}
            stroke="rgba(148,163,184,0.4)"
            strokeWidth="1.5"
            markerEnd="url(#arrow)"
          />
        )
      })}

      {/* Nodes */}
      {nodes.map((node) => {
        const fill = TYPE_FILL[node.type] || '#64748b'
        const isRoot = node.componentId === rootId
        return (
          <g key={node.componentId} transform={`translate(${node.x},${node.y})`}>
            {isRoot && (
              <circle
                r={NODE_R + 6}
                fill="none"
                stroke={fill}
                strokeWidth="1.5"
                strokeDasharray="4 3"
                opacity="0.5"
              />
            )}
            <circle
              r={NODE_R}
              fill={fill}
              fillOpacity={isRoot ? 0.9 : 0.7}
              stroke={fill}
              strokeWidth={isRoot ? 2 : 1}
            />
            <text
              textAnchor="middle"
              dominantBaseline="middle"
              fill="#fff"
              fontSize="9.5"
              fontWeight="600"
              fontFamily="Inter, system-ui, sans-serif"
            >
              {truncate(node.name, 10)}
            </text>
            <text
              textAnchor="middle"
              y={NODE_R + 14}
              fill="rgba(148,163,184,0.85)"
              fontSize="9"
              fontFamily="Inter, system-ui, sans-serif"
            >
              {node.type}
            </text>
          </g>
        )
      })}
    </svg>
  )
}
