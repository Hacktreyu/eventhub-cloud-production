import { useState, useEffect, useCallback } from 'react'

// URL base de la API - configurable vía variable de entorno
const API_BASE = import.meta.env.VITE_API_URL || ''

function App() {
    const [events, setEvents] = useState([])
    const [stats, setStats] = useState(null)
    const [loading, setLoading] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const [toast, setToast] = useState(null)

    const [formData, setFormData] = useState({
        title: '',
        description: '',
        source: 'web-app',
        type: 'USER_ACTION'
    })

    // Obtener eventos
    const fetchEvents = useCallback(async () => {
        try {
            const response = await fetch(`${API_BASE}/api/events`)
            if (response.ok) {
                const data = await response.json()
                setEvents(data)
            }
        } catch (error) {
            console.error('Error fetching events:', error)
        }
    }, [])

    // Obtener estadísticas
    const fetchStats = useCallback(async () => {
        try {
            const response = await fetch(`${API_BASE}/api/events/stats`)
            if (response.ok) {
                const data = await response.json()
                setStats(data)
            }
        } catch (error) {
            console.error('Error fetching stats:', error)
        }
    }, [])

    // Carga inicial y polling cada 3 segundos
    useEffect(() => {
        setLoading(true)
        Promise.all([fetchEvents(), fetchStats()]).finally(() => setLoading(false))

        // Polling cada 3 segundos
        const interval = setInterval(() => {
            fetchEvents()
            fetchStats()
        }, 3000)

        return () => clearInterval(interval)
    }, [fetchEvents, fetchStats])

    // Mostrar notificación toast
    const showToast = (message, type = 'success') => {
        setToast({ message, type })
        setTimeout(() => setToast(null), 3000)
    }

    // Manejar envío del formulario
    const handleSubmit = async (e) => {
        e.preventDefault()

        if (!formData.title.trim()) {
            showToast('Title is required', 'error')
            return
        }

        setSubmitting(true)

        try {
            const response = await fetch(`${API_BASE}/api/events`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData),
            })

            if (response.ok) {
                const newEvent = await response.json()
                showToast(`Event #${newEvent.id} created successfully!`, 'success')
                setFormData({ ...formData, title: '', description: '' })
                fetchEvents()
                fetchStats()
            } else {
                const error = await response.json()
                showToast(error.message || 'Failed to create event', 'error')
            }
        } catch (error) {
            showToast('Connection error. Is the API running?', 'error')
        } finally {
            setSubmitting(false)
        }
    }

    // Actualizar campo del formulario
    const handleChange = (e) => {
        const { name, value } = e.target
        setFormData(prev => ({ ...prev, [name]: value }))
    }

    // Limpiar todos los eventos
    const handleClearEvents = async () => {
        if (!confirm('Are you sure you want to delete ALL events?')) return

        setLoading(true)
        try {
            const response = await fetch(`${API_BASE}/api/events`, {
                method: 'DELETE',
            })

            if (response.ok) {
                showToast('All events cleared successfully!', 'success')
                fetchEvents()
                fetchStats()
            } else {
                showToast('Failed to clear events', 'error')
            }
        } catch (error) {
            showToast('Connection error', 'error')
        } finally {
            setLoading(false)
        }
    }

    // Formatear fecha
    const formatDate = (dateString) => {
        if (!dateString) return '-'
        const date = new Date(dateString)
        return date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        })
    }

    return (
        <>
            {/* Header */}
            <header className="header">
                <div className="header__logo">
                    <div className="header__logo-icon">⚡</div>
                    <span className="header__logo-text">EventHub Cloud</span>
                </div>
                <div className="header__status">
                    <span
                        className={`header__status-dot ${stats?.kafkaEnabled ? 'header__status-dot--kafka' : 'header__status-dot--demo'}`}
                    />
                    <span>
                        {stats?.kafkaEnabled ? 'Kafka Mode' : 'Demo Mode (In-Memory)'}
                    </span>
                </div>
            </header>

            {/* Main Content */}
            <main className="main">
                {/* Stats Cards */}
                <div className="stats">
                    <div className="stat-card">
                        <span className="stat-card__label">Total Events</span>
                        <span className="stat-card__value stat-card__value--total">
                            {stats?.total ?? '-'}
                        </span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-card__label">Pending</span>
                        <span className="stat-card__value stat-card__value--pending">
                            {stats?.pending ?? '-'}
                        </span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-card__label">Processed</span>
                        <span className="stat-card__value stat-card__value--processed">
                            {stats?.processed ?? '-'}
                        </span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-card__label">Failed</span>
                        <span className="stat-card__value stat-card__value--failed">
                            {stats?.failed ?? '-'}
                        </span>
                    </div>
                </div>

                {/* Content Grid */}
                <div className="content-grid">
                    {/* Create Event Form */}
                    <section className="form-section">
                        <h2 className="form-section__title">
                            📝 Create Event
                        </h2>
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label htmlFor="title">Title *</label>
                                <input
                                    type="text"
                                    id="title"
                                    name="title"
                                    value={formData.title}
                                    onChange={handleChange}
                                    placeholder="Enter event title..."
                                    maxLength={200}
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="description">Description</label>
                                <textarea
                                    id="description"
                                    name="description"
                                    value={formData.description}
                                    onChange={handleChange}
                                    placeholder="Optional description..."
                                    maxLength={1000}
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="type">Event Type</label>
                                <select
                                    id="type"
                                    name="type"
                                    value={formData.type}
                                    onChange={handleChange}
                                >
                                    <option value="USER_ACTION">User Action</option>
                                    <option value="SYSTEM_EVENT">System Event</option>
                                    <option value="NOTIFICATION">Notification</option>
                                    <option value="DATA_UPDATE">Data Update</option>
                                    <option value="INTEGRATION">Integration</option>
                                </select>
                            </div>
                            <div className="form-group">
                                <label htmlFor="source">Source</label>
                                <input
                                    type="text"
                                    id="source"
                                    name="source"
                                    value={formData.source}
                                    onChange={handleChange}
                                    placeholder="Event source..."
                                    maxLength={100}
                                />
                            </div>
                            <button
                                type="submit"
                                className="btn btn--primary btn--full"
                                disabled={submitting}
                            >
                                {submitting ? (
                                    <>
                                        <span className="spinner" />
                                        Creating...
                                    </>
                                ) : (
                                    <>🚀 Publish Event</>
                                )}
                            </button>
                        </form>
                    </section>

                    {/* Events Table */}
                    <section className="table-section">
                        <div className="table-section__header">
                            <h2 className="table-section__title">
                                📋 Events
                            </h2>
                            <span style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                                <button
                                    onClick={handleClearEvents}
                                    className="btn"
                                    style={{
                                        backgroundColor: 'var(--color-accent-error)',
                                        color: 'white',
                                        padding: '0.25rem 0.75rem',
                                        fontSize: '0.8rem'
                                    }}
                                    disabled={loading || events.length === 0}
                                >
                                    🗑️ Clear All
                                </button>
                                <span className="table-section__refresh">
                                    {loading ? <span className="spinner" /> : '🔄'}
                                    Auto-refresh: 3s
                                </span>
                            </span>
                        </div>

                        {events.length === 0 ? (
                            <div className="empty-state">
                                <div className="empty-state__icon">📭</div>
                                <p className="empty-state__text">
                                    No events yet. Create your first event!
                                </p>
                            </div>
                        ) : (
                            <div className="table-wrapper">
                                <table className="events-table">
                                    <thead>
                                        <tr>
                                            <th>ID</th>
                                            <th>Title</th>
                                            <th>Type</th>
                                            <th>Status</th>
                                            <th>Created</th>
                                            <th>Processed</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {events.map(event => (
                                            <tr key={event.id}>
                                                <td className="id-col">#{event.id}</td>
                                                <td className="title-col" title={event.title}>
                                                    {event.title}
                                                </td>
                                                <td className="type-col">{event.type}</td>
                                                <td>
                                                    <span className={`status-badge status-badge--${event.status.toLowerCase()}`}>
                                                        {event.status}
                                                    </span>
                                                </td>
                                                <td className="time-col">{formatDate(event.createdAt)}</td>
                                                <td className="time-col">{formatDate(event.processedAt)}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </section>
                </div>
            </main>

            {/* Footer */}
            <footer className="footer">
                <p>
                    EventHub Cloud Demo —
                    <a href="https://github.com/YOUR_USERNAME/eventhub-cloud" target="_blank" rel="noopener noreferrer">
                        View on GitHub
                    </a>
                    {' '} | Built with Java Spring Boot + React
                </p>
            </footer>

            {/* Toast Notification */}
            {toast && (
                <div className={`toast toast--${toast.type}`}>
                    {toast.type === 'success' ? '✅' : '❌'} {toast.message}
                </div>
            )}
        </>
    )
}

export default App
