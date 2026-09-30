import React from 'react';
import { Router, Radio, Activity, CheckCircle, Zap, ArrowUpRight, Cpu } from 'lucide-react';
import { Tr069Device, Tr069Metrics, isDeviceOnline } from '../types/tr069';

interface DashboardViewProps {
  devices: Tr069Device[];
  metrics: Tr069Metrics | null;
  onSelectDevice: (device: Tr069Device) => void;
  onNavigateTab: (tab: string) => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  devices,
  metrics,
  onSelectDevice,
  onNavigateTab,
}) => {
  const onlineCount = devices.filter(isDeviceOnline).length;
  const offlineCount = devices.length - onlineCount;

  const totalRpc = (metrics?.totalRpcSuccess || 0) + (metrics?.totalRpcFaults || 0);
  const successRate = totalRpc > 0
    ? Math.round(((metrics?.totalRpcSuccess || 0) / totalRpc) * 100)
    : 100;

  return (
    <div>
      {/* KPI Cards */}
      <div className="stat-grid">
        <div className="card stat-card">
          <div className="stat-info">
            <span className="stat-label">Managed CPEs</span>
            <span className="stat-value">{devices.length}</span>
            <span className="stat-subtext" style={{ color: '#34d399' }}>
              ● {onlineCount} online {offlineCount > 0 ? `| ${offlineCount} offline` : ''}
            </span>
          </div>
          <div className="stat-icon-wrapper">
            <Router size={24} />
          </div>
        </div>

        <div className="card stat-card">
          <div className="stat-info">
            <span className="stat-label">Active CWMP Sessions</span>
            <span className="stat-value">{metrics?.activeSessions ?? 0}</span>
            <span className="stat-subtext">With expiration reaper</span>
          </div>
          <div className="stat-icon-wrapper success">
            <Activity size={24} />
          </div>
        </div>

        <div className="card stat-card">
          <div className="stat-info">
            <span className="stat-label">Processed Informs</span>
            <span className="stat-value">{metrics?.totalInforms ?? 0}</span>
            <span className="stat-subtext">
              {metrics?.bootstrapEvents ?? 0} bootstraps | {metrics?.periodicEvents ?? 0} periodics
            </span>
          </div>
          <div className="stat-icon-wrapper indigo">
            <Radio size={24} />
          </div>
        </div>

        <div className="card stat-card">
          <div className="stat-info">
            <span className="stat-label">RPC Success Rate</span>
            <span className="stat-value">{successRate}%</span>
            <span className="stat-subtext">
              {metrics?.totalRpcSuccess ?? 0} successes | {metrics?.totalRpcFaults ?? 0} faults
            </span>
          </div>
          <div className="stat-icon-wrapper warning">
            <CheckCircle size={24} />
          </div>
        </div>
      </div>

      {/* Main Grid: Live CPEs + Architecture Status */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '24px', marginBottom: '28px' }}>
        {/* CPE Inventory summary */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
            <div>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 600 }}>Operational CPEs</h3>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                TR-069 devices associated and synchronized with Omada topology
              </p>
            </div>
            <button
              onClick={() => onNavigateTab('devices')}
              className="btn btn-secondary btn-sm"
              style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
            >
              View all <ArrowUpRight size={14} />
            </button>
          </div>

          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Device</th>
                  <th>MAC Address</th>
                  <th>IP / WAN</th>
                  <th>Status</th>
                  <th>Clients</th>
                  <th style={{ textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {devices.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', padding: '36px', color: 'var(--text-dim)' }}>
                      No TR-069 devices registered yet. Configure the ACS URL on the router/ONT.
                    </td>
                  </tr>
                ) : (
                  devices.map((device) => (
                    <tr key={device.mac}>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <div style={{
                            width: '32px', height: '32px', borderRadius: '8px',
                            background: 'rgba(6, 182, 212, 0.1)', display: 'flex',
                            alignItems: 'center', justifyContent: 'center', color: 'var(--primary)'
                          }}>
                            <Router size={16} />
                          </div>
                          <div>
                            <div style={{ fontWeight: 600 }}>{device.modelName || 'Generic CPE'}</div>
                            <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>
                              {device.manufacturer || 'ZTE / Multilaser'}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <span className="font-mono">{device.mac}</span>
                      </td>
                      <td>
                        <span className="font-mono" style={{ color: 'var(--text-muted)' }}>
                          {device.ip || '—'}
                        </span>
                      </td>
                      <td>
                        {isDeviceOnline(device) ? (
                          <span className="badge badge-online">
                            <span className="pulse-dot"></span> Online
                          </span>
                        ) : (
                          <span className="badge badge-offline">Offline</span>
                        )}
                      </td>
                      <td>
                        <span style={{ fontWeight: 600, color: 'var(--primary)' }}>
                          {(device.wlanClientsCount || 0) + (device.lanClientsCount || 0)}
                        </span>
                        <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginLeft: '4px' }}>
                          client(s)
                        </span>
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <button
                          onClick={() => onSelectDevice(device)}
                          className="btn btn-secondary btn-sm"
                        >
                          Manage
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* ACS Server Status Overview */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div className="card">
            <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Zap size={18} color="var(--primary)" />
              ACS Server Status
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-muted)' }}>ACS URL:</span>
                <span className="font-mono" style={{ color: 'var(--primary)' }}>http://&lt;omada-ip&gt;:8088/tr069</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-muted)' }}>CWMP Shiro Security:</span>
                <span style={{ color: '#34d399', fontWeight: 600 }}>PermitAll (Bypass Active)</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-muted)' }}>Omada Memory Cache:</span>
                <span style={{ color: '#34d399', fontWeight: 600 }}>Real-time Synchronized</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-muted)' }}>ClientImage Grid:</span>
                <span style={{ color: '#34d399', fontWeight: 600 }}>Active (No aZ/NPE Fault)</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Time-series DB:</span>
                <span className="font-mono" style={{ color: 'var(--text-main)' }}>omada_data (apstat5min)</span>
              </div>
            </div>
          </div>

          <div className="card" style={{ background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.08) 0%, rgba(99, 102, 241, 0.05) 100%)' }}>
            <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Cpu size={16} color="var(--primary)" />
              Quick Provisioning Guide
            </h4>
            <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', lineHeight: '1.4' }}>
              To bring up a new ONT (such as ZTE F680 or F6600P), enable <strong>TR069_INTERNET</strong> on the active WAN and point to port <code>8088/tr069</code> with a 10s interval.
            </p>
            <button
              onClick={() => onNavigateTab('config')}
              className="btn btn-secondary btn-sm"
              style={{ marginTop: '12px', width: '100%' }}
            >
              View Full CPE Instructions
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
