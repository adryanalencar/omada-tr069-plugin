import React, { useState } from 'react';
import { Server, RefreshCw, Settings, CheckCircle2, AlertCircle } from 'lucide-react';
import { getAcsHost, setAcsHost } from '../services/api';

interface HeaderProps {
  onRefresh: () => void;
  isRefreshing: boolean;
  refreshInterval: number;
  onIntervalChange: (interval: number) => void;
  onlineDevicesCount: number;
}

export const Header: React.FC<HeaderProps> = ({
  onRefresh,
  isRefreshing,
  refreshInterval,
  onIntervalChange,
  onlineDevicesCount,
}) => {
  const [showSettingsModal, setShowSettingsModal] = useState(false);
  const [hostInput, setHostInput] = useState(getAcsHost());
  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleSaveHost = () => {
    setAcsHost(hostInput);
    setSavedSuccess(true);
    setTimeout(() => {
      setSavedSuccess(false);
      setShowSettingsModal(false);
      onRefresh();
    }, 600);
  };

  return (
    <>
      <header className="app-header">
        <div className="brand-section">
          <div className="brand-logo-icon">
            <Server size={22} />
          </div>
          <div className="brand-title">
            OpenOmada <span style={{ color: 'var(--primary)', fontWeight: 600 }}>TR-069</span>
            <span className="version-badge">Enterprise v1.0</span>
          </div>
        </div>

        <div className="header-actions">
          <div className="status-pill">
            <span className="pulse-dot"></span>
            <span>ACS Online ({onlineDevicesCount} CPE{onlineDevicesCount !== 1 ? 's' : ''} active)</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Auto-sync:</span>
            <select
              value={refreshInterval}
              onChange={(e) => onIntervalChange(Number(e.target.value))}
              className="input-control"
              style={{ width: 'auto', padding: '5px 10px', fontSize: '0.78rem' }}
            >
              <option value={0}>Off</option>
              <option value={5000}>5s</option>
              <option value={10000}>10s</option>
              <option value={30000}>30s</option>
            </select>
          </div>

          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="btn btn-secondary btn-sm"
            title="Refresh now"
          >
            <RefreshCw size={14} className={isRefreshing ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>

          <button
            onClick={() => setShowSettingsModal(true)}
            className="btn btn-secondary btn-sm"
            title="Configure ACS Endpoint"
          >
            <Settings size={14} />
            <span>Connection</span>
          </button>
        </div>
      </header>

      {showSettingsModal && (
        <div className="modal-overlay" onClick={() => setShowSettingsModal(false)}>
          <div className="modal-content" style={{ maxWidth: '520px' }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 style={{ fontSize: '1rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Settings size={18} color="var(--primary)" />
                Configure ACS Server Host
              </h3>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: '0.84rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
                Set the base URL for the Omada Controller / TR-069 server. Leave blank if the UI is running on the same host (port 8088).
              </p>

              <label style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                ACS Host URL:
              </label>
              <input
                type="text"
                className="input-control font-mono"
                placeholder="e.g. http://2.24.202.99:8088 or blank for relative"
                value={hostInput}
                onChange={(e) => setHostInput(e.target.value)}
              />

              <div style={{ marginTop: '14px', padding: '12px', background: 'rgba(6, 182, 212, 0.08)', borderRadius: 'var(--radius-md)', border: '1px solid rgba(6, 182, 212, 0.2)' }}>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <AlertCircle size={14} color="var(--primary)" />
                  <strong>Development Tip:</strong>
                </div>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                  In local development mode (Vite), the proxy automatically redirects API calls to the public Omada IP (<code>2.24.202.99:8088</code>).
                </div>
              </div>

              {savedSuccess && (
                <div style={{ marginTop: '12px', color: 'var(--success)', fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <CheckCircle2 size={16} /> Settings saved successfully!
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setShowSettingsModal(false)}>
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleSaveHost}>
                Save & Reconnect
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
