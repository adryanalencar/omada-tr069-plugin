import React, { useState } from 'react';
import { getAcsHost, setAcsHost } from '../services/api';
import { OmadaButton } from './OmadaButton';
import { Settings, RefreshCw, Radio, Server, Check } from 'lucide-react';

interface OmadaHeaderProps {
  onlineCount: number;
  totalCount: number;
  refreshInterval: number;
  onRefreshIntervalChange: (sec: number) => void;
  onManualRefresh: () => void;
  loading: boolean;
}

export const OmadaHeader: React.FC<OmadaHeaderProps> = ({
  onlineCount,
  totalCount,
  refreshInterval,
  onRefreshIntervalChange,
  onManualRefresh,
  loading,
}) => {
  const [showHostModal, setShowHostModal] = useState(false);
  const [customHost, setCustomHost] = useState(getAcsHost());

  const handleSaveHost = () => {
    setAcsHost(customHost);
    setShowHostModal(false);
    window.location.reload();
  };

  return (
    <header className="omada-header-bar">
      {/* Brand & Breadcrumb */}
      <div className="omada-brand">
        <div className="omada-brand-logo">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="24" height="24" rx="6" fill="#00A870"/>
            <path d="M7 12L10.5 15.5L17 9" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          <span>TP-Link Omada</span>
          <span className="omada-brand-badge">TR-069 NATIVE</span>
        </div>

        <div style={{ width: '1px', height: '18px', background: '#ECECEC', margin: '0 8px' }} />

        <div className="omada-breadcrumb">
          <span>Settings</span>
          <span className="omada-breadcrumb-sep">/</span>
          <span>Network & Devices</span>
          <span className="omada-breadcrumb-sep">/</span>
          <span className="omada-breadcrumb-current">TR-069 ACS (CWMP)</span>
        </div>
      </div>

      {/* Right Controls */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {/* Status indicator */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px' }}>
          <span 
            style={{ 
              display: 'inline-block', 
              width: '8px', 
              height: '8px', 
              borderRadius: '50%', 
              background: '#05C178',
              boxShadow: '0 0 0 2px rgba(5, 193, 120, 0.2)' 
            }} 
          />
          <span style={{ color: '#636363' }}>
            Online CPEs: <strong style={{ color: '#2B2B2B' }}>{onlineCount}</strong> / {totalCount}
          </span>
        </div>

        {/* Auto Refresh Select */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#636363' }}>
          <span>Auto:</span>
          <select
            className="omada-input"
            style={{ height: '30px', padding: '2px 8px', fontSize: '12px' }}
            value={refreshInterval}
            onChange={(e) => onRefreshIntervalChange(Number(e.target.value))}
          >
            <option value={10}>10s</option>
            <option value={30}>30s</option>
            <option value={60}>60s</option>
            <option value={0}>Disabled</option>
          </select>
        </div>

        {/* Refresh Button */}
        <OmadaButton
          variant="default"
          onClick={onManualRefresh}
          disabled={loading}
          icon={<RefreshCw size={13} className={loading ? 'spin-anim' : ''} />}
        >
          {loading ? 'Refreshing...' : 'Refresh'}
        </OmadaButton>

        {/* Host Setting Button */}
        <button
          onClick={() => setShowHostModal(true)}
          title="Configure ACS Host"
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: '#636363',
            padding: '6px',
            borderRadius: '4px',
            display: 'flex',
            alignItems: 'center',
          }}
        >
          <Settings size={18} />
        </button>
      </div>

      {/* Host Modal */}
      {showHostModal && (
        <div className="omada-modal-overlay">
          <div className="omada-modal" style={{ maxWidth: '480px' }}>
            <div className="omada-modal-header">
              <h2 className="omada-modal-title">Configure ACS Server Host</h2>
              <button className="omada-modal-close" onClick={() => setShowHostModal(false)}>✕</button>
            </div>
            <div className="omada-modal-body" style={{ padding: '20px' }}>
              <p style={{ fontSize: '13px', color: '#636363', marginBottom: '14px' }}>
                Set the base URL and port where the Omada Controller with TR-069 service is running:
              </p>
              <input
                type="text"
                className="omada-input"
                style={{ width: '100%', marginBottom: '12px' }}
                placeholder="Ex: http://2.24.202.99:8088"
                value={customHost}
                onChange={(e) => setCustomHost(e.target.value)}
              />
              <div style={{ fontSize: '11px', color: '#999999' }}>
                Leave blank to use the same origin (relative paths) when served directly from Omada.
              </div>
            </div>
            <div className="omada-modal-footer">
              <OmadaButton variant="default" onClick={() => setShowHostModal(false)}>
                Cancel
              </OmadaButton>
              <OmadaButton variant="primary" onClick={handleSaveHost}>
                Save & Reconnect
              </OmadaButton>
            </div>
          </div>
        </div>
      )}
    </header>
  );
};
