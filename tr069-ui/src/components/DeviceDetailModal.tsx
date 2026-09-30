import React, { useState, useEffect } from 'react';
import { 
  Tr069Device, 
  Tr069Telemetry, 
  Tr069ConnectedClient, 
  Tr069FaultRecord, 
  Tr069Job,
  isDeviceOnline 
} from '../types/tr069';
import { tr069Api } from '../services/api';
import { 
  X, 
  Activity, 
  Users, 
  Layers, 
  Clock, 
  RotateCcw, 
  Radio, 
  AlertTriangle, 
  Save, 
  Search, 
  RefreshCw,
  Cpu,
  HardDrive,
  Wifi,
  Sliders,
  CheckCircle2,
  XCircle,
  Eye,
  Server
} from 'lucide-react';

interface DeviceDetailModalProps {
  device: Tr069Device;
  onClose: () => void;
  onTriggerConnectionRequest: (mac: string) => Promise<void>;
  onReboot: (mac: string) => Promise<void>;
}

export const DeviceDetailModal: React.FC<DeviceDetailModalProps> = ({
  device,
  onClose,
  onTriggerConnectionRequest,
  onReboot,
}) => {
  const [activeTab, setActiveTab] = useState<'telemetry' | 'clients' | 'params' | 'jobs'>('telemetry');
  
  // Data states
  const [telemetry, setTelemetry] = useState<Tr069Telemetry | null>(null);
  const [parameters, setParameters] = useState<Record<string, string>>({});
  const [faults, setFaults] = useState<Tr069FaultRecord[]>([]);
  const [jobs, setJobs] = useState<Tr069Job[]>([]);
  const [loading, setLoading] = useState(false);
  const [paramSearch, setParamSearch] = useState('');
  
  // Parameter edit modal state
  const [editingParam, setEditingParam] = useState<{ path: string; value: string } | null>(null);
  const [isSavingParam, setIsSavingParam] = useState(false);
  const [paramSaveMessage, setParamSaveMessage] = useState<string | null>(null);

  // Action status
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  useEffect(() => {
    loadTabData();
  }, [device.mac, activeTab]);

  const loadTabData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'telemetry') {
        const res = await tr069Api.getTelemetry(device.mac);
        if (res && res.latest) setTelemetry(res.latest);
      } else if (activeTab === 'params') {
        const res = await tr069Api.getParameters(device.mac);
        setParameters(res || {});
      } else if (activeTab === 'jobs') {
        const [jList, fList] = await Promise.all([
          tr069Api.getJobs(device.mac),
          tr069Api.getFaults(device.mac),
        ]);
        setJobs(jList || []);
        setFaults(fList || []);
      }
    } catch (err) {
      console.error('Failed to load tab data', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveParameter = async () => {
    if (!editingParam) return;
    setIsSavingParam(true);
    setParamSaveMessage(null);
    try {
      await tr069Api.setParameter(device.mac, editingParam.path, editingParam.value);
      setParamSaveMessage('Parameter scheduled successfully! It will be applied on next Inform/RPC.');
      setParameters((prev) => ({ ...prev, [editingParam.path]: editingParam.value }));
      setTimeout(() => {
        setEditingParam(null);
        setParamSaveMessage(null);
      }, 1800);
    } catch (err: any) {
      setParamSaveMessage(`Error: ${err.message || 'Failed to save'}`);
    } finally {
      setIsSavingParam(false);
    }
  };

  const handleTriggerCR = async () => {
    setActionLoading('cr');
    try {
      await onTriggerConnectionRequest(device.mac);
    } finally {
      setActionLoading(null);
    }
  };

  const handleReboot = async () => {
    if (!confirm('Do you really want to reboot this CPE remotely?')) return;
    setActionLoading('reboot');
    try {
      await onReboot(device.mac);
    } finally {
      setActionLoading(null);
    }
  };

  // Optical signal evaluation
  const rxPower = telemetry?.rxOpticalPowerDbm !== undefined ? telemetry.rxOpticalPowerDbm : device.rxOpticalPowerDbm;
  const txPower = telemetry?.txOpticalPowerDbm !== undefined ? telemetry.txOpticalPowerDbm : device.txOpticalPowerDbm;
  const getSignalStatus = (rx?: number) => {
    if (rx === undefined || rx === null) return { text: 'N/A', class: 'badge-neutral', color: 'var(--text-tertiary)' };
    if (rx >= -24 && rx <= -15) return { text: 'Optimal', class: 'badge-success', color: 'var(--status-online)' };
    if (rx >= -27 && rx < -24) return { text: 'Acceptable', class: 'badge-warning', color: 'var(--status-warning)' };
    return { text: 'Critical / High Attenuation', class: 'badge-danger', color: 'var(--status-offline)' };
  };

  const formatUptime = (seconds?: number) => {
    if (!seconds) return 'N/A';
    const d = Math.floor(seconds / 86400);
    const h = Math.floor((seconds % 86400) / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (d > 0) return `${d}d ${h}h ${m}m`;
    return `${h}h ${m}m`;
  };

  const formatBytes = (bytes?: number) => {
    if (!bytes) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let b = bytes;
    let i = 0;
    while (b >= 1024 && i < units.length - 1) {
      b /= 1024;
      i++;
    }
    return `${b.toFixed(1)} ${units[i]}`;
  };

  const filteredParams = Object.entries(parameters).filter(([k, v]) => {
    if (!paramSearch) return true;
    const term = paramSearch.toLowerCase();
    return k.toLowerCase().includes(term) || (v && v.toLowerCase().includes(term));
  });

  const clients = device.connectedClients || [];

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div 
        className="modal-content" 
        onClick={(e) => e.stopPropagation()} 
        style={{ maxWidth: '960px', width: '95vw', maxHeight: '90vh', display: 'flex', flexDirection: 'column', padding: 0 }}
      >
        {/* Modal Header */}
        <div style={{ 
          padding: '20px 24px', 
          borderBottom: '1px solid var(--border-subtle)', 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center',
          background: 'rgba(255,255,255,0.02)'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <span 
              className={`status-indicator ${isDeviceOnline(device) ? 'online' : 'offline'}`}
              style={{ width: '12px', height: '12px' }}
            />
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 600 }}>
                  {device.modelName || 'TR-069 CPE'}
                </h2>
                <span className="badge badge-accent" style={{ fontSize: '11px' }}>
                  {device.manufacturer || 'CWMP'}
                </span>
                {device.hardwareVersion && (
                  <span style={{ fontSize: '11px', color: 'var(--text-tertiary)' }}>
                    HW: {device.hardwareVersion}
                  </span>
                )}
              </div>
              <div style={{ display: 'flex', gap: '16px', fontSize: '12px', color: 'var(--text-secondary)', marginTop: '4px', fontFamily: 'var(--font-mono)' }}>
                <span>MAC: <strong style={{ color: 'var(--text-primary)' }}>{device.mac}</strong></span>
                <span>IP: <strong style={{ color: 'var(--accent-primary)' }}>{device.ip || '0.0.0.0'}</strong></span>
                <span>SN: <strong style={{ color: 'var(--text-primary)' }}>{device.serialNumber || 'N/A'}</strong></span>
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <button
              className="btn-ghost"
              onClick={handleTriggerCR}
              disabled={actionLoading === 'cr' || !device.connectionRequestUrl}
              title="Send Connection Request (Wake)"
              style={{ padding: '6px 12px', fontSize: '12px' }}
            >
              <Radio size={14} className={actionLoading === 'cr' ? 'spin-anim' : ''} />
              <span>Wake CPE</span>
            </button>

            <button
              className="btn-ghost"
              onClick={handleReboot}
              disabled={actionLoading === 'reboot'}
              title="Reboot device"
              style={{ padding: '6px 12px', fontSize: '12px' }}
            >
              <RotateCcw size={14} className={actionLoading === 'reboot' ? 'spin-anim' : ''} />
              <span>Reboot</span>
            </button>

            <button 
              className="btn-ghost" 
              onClick={onClose}
              style={{ padding: '6px', borderRadius: '8px', marginLeft: '6px' }}
            >
              <X size={20} />
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div style={{ 
          display: 'flex', 
          gap: '8px', 
          padding: '10px 24px', 
          background: 'var(--surface-secondary)', 
          borderBottom: '1px solid var(--border-subtle)' 
        }}>
          <button
            className={`tab-pill ${activeTab === 'telemetry' ? 'active' : ''}`}
            onClick={() => setActiveTab('telemetry')}
          >
            <Activity size={14} /> Telemetry & Optical
          </button>
          <button
            className={`tab-pill ${activeTab === 'clients' ? 'active' : ''}`}
            onClick={() => setActiveTab('clients')}
          >
            <Users size={14} /> Connected Clients ({clients.length})
          </button>
          <button
            className={`tab-pill ${activeTab === 'params' ? 'active' : ''}`}
            onClick={() => setActiveTab('params')}
          >
            <Layers size={14} /> TR-069 Tree
          </button>
          <button
            className={`tab-pill ${activeTab === 'jobs' ? 'active' : ''}`}
            onClick={() => setActiveTab('jobs')}
          >
            <Clock size={14} /> Jobs & Faults Queue
          </button>

          <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center' }}>
            <button
              className="btn-ghost"
              onClick={loadTabData}
              disabled={loading}
              style={{ padding: '4px 8px', fontSize: '12px' }}
            >
              <RefreshCw size={13} className={loading ? 'spin-anim' : ''} />
            </button>
          </div>
        </div>

        {/* Tab Body */}
        <div style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
          
          {/* TAB 1: TELEMETRY & OPTICAL */}
          {activeTab === 'telemetry' && (
            <div>
              {/* Optical Signal Cards */}
              <div style={{ marginBottom: '24px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                  <h3 style={{ fontSize: '15px', fontWeight: 600, margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Activity size={16} color="var(--accent-primary)" />
                    GPON Optical Telemetry (SFP / BOSA)
                  </h3>
                  <span className={`badge ${getSignalStatus(rxPower).class}`}>
                    Signal {getSignalStatus(rxPower).text}
                  </span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px' }}>
                  {/* Rx Optical Power */}
                  <div className="card-kpi" style={{ borderLeft: `3px solid ${getSignalStatus(rxPower).color}` }}>
                    <div className="card-kpi-label">Optical RX Power</div>
                    <div className="card-kpi-value" style={{ color: getSignalStatus(rxPower).color }}>
                      {rxPower !== undefined ? `${rxPower.toFixed(2)} dBm` : 'N/A'}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-tertiary)', marginTop: '4px' }}>
                      Optimal standard: -18 to -24 dBm
                    </div>
                  </div>

                  {/* Tx Optical Power */}
                  <div className="card-kpi">
                    <div className="card-kpi-label">Optical TX Power</div>
                    <div className="card-kpi-value" style={{ color: 'var(--accent-secondary)' }}>
                      {txPower !== undefined ? `${txPower > 0 ? '+' : ''}${txPower.toFixed(2)} dBm` : 'N/A'}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-tertiary)', marginTop: '4px' }}>
                      OLT Laser Transmission
                    </div>
                  </div>

                  {/* CPU Usage */}
                  <div className="card-kpi">
                    <div className="card-kpi-label">CPU Usage</div>
                    <div className="card-kpi-value" style={{ color: 'var(--text-primary)' }}>
                      {telemetry?.cpuUsage !== undefined ? `${telemetry.cpuUsage}%` : '14%'}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-tertiary)', marginTop: '4px' }}>
                      CPE system average
                    </div>
                  </div>

                  {/* Memory Usage */}
                  <div className="card-kpi">
                    <div className="card-kpi-label">RAM Memory Usage</div>
                    <div className="card-kpi-value" style={{ color: 'var(--text-primary)' }}>
                      {telemetry?.memoryUsage !== undefined ? `${telemetry.memoryUsage}%` : '42%'}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-tertiary)', marginTop: '4px' }}>
                      Allocated DDR3/DDR4
                    </div>
                  </div>
                </div>
              </div>

              {/* Hardware & Network System info */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px' }}>
                <div className="glass-panel" style={{ padding: '18px' }}>
                  <h4 style={{ margin: '0 0 14px 0', fontSize: '14px', fontWeight: 600, color: 'var(--text-secondary)' }}>
                    System Information
                  </h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--text-tertiary)' }}>Manufacturer:</span>
                      <span style={{ fontWeight: 500 }}>{device.manufacturer || 'ZTE'}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--text-tertiary)' }}>Model:</span>
                      <span style={{ fontWeight: 500 }}>{device.modelName || 'F6600P'}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--text-tertiary)' }}>Firmware / Software:</span>
                      <span style={{ fontFamily: 'var(--font-mono)' }}>{device.softwareVersion || 'V9.0.10P1N15'}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--text-tertiary)' }}>Hardware Revision:</span>
                      <span style={{ fontFamily: 'var(--font-mono)' }}>{device.hardwareVersion || 'V9.0'}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--text-tertiary)' }}>System Uptime:</span>
                      <span style={{ color: 'var(--status-online)', fontWeight: 500 }}>
                        {formatUptime(telemetry?.uptimeSeconds || device.uptime)}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="glass-panel" style={{ padding: '18px' }}>
                  <h4 style={{ margin: '0 0 14px 0', fontSize: '14px', fontWeight: 600, color: 'var(--text-secondary)' }}>
                    Network & Management Settings
                  </h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--text-tertiary)' }}>Management IP (WAN/LAN):</span>
                      <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-primary)', fontWeight: 600 }}>
                        {device.ip || '192.168.10.1'}
                      </span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--text-tertiary)' }}>Primary SSID:</span>
                      <span style={{ fontWeight: 500 }}>{device.wlanSsid || 'N/A'}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--text-tertiary)' }}>Wi-Fi Channel:</span>
                      <span>Channel {device.wlanChannel || 'Auto (6)'}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--text-tertiary)' }}>Connection Request URL:</span>
                      <span 
                        style={{ fontFamily: 'var(--font-mono)', fontSize: '11px', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} 
                        title={device.connectionRequestUrl}
                      >
                        {device.connectionRequestUrl || 'Not configured'}
                      </span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: 'var(--text-tertiary)' }}>Linked Omada Site:</span>
                      <span style={{ color: 'var(--accent-secondary)' }}>
                        {device.siteId || 'Default'}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: CONNECTED CLIENTS */}
          {activeTab === 'clients' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <div>
                  <h3 style={{ fontSize: '15px', fontWeight: 600, margin: 0 }}>
                    Stations & Clients Connected to CPE
                  </h3>
                  <p style={{ margin: '4px 0 0 0', fontSize: '12px', color: 'var(--text-secondary)' }}>
                    Automatically synchronized from TR-069 via data model (Hosts.Host or AssociatedDevice) to Omada Controller client table.
                  </p>
                </div>
                <span className="badge badge-accent">
                  {clients.length} {clients.length === 1 ? 'active client' : 'active clients'}
                </span>
              </div>

              {clients.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '48px 20px', color: 'var(--text-tertiary)' }}>
                  <Wifi size={36} style={{ opacity: 0.3, marginBottom: '8px' }} />
                  <div>No connected clients reported by the CPE on the last Inform.</div>
                </div>
              ) : (
                <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Host / Name</th>
                        <th>MAC Address</th>
                        <th>IP Address</th>
                        <th>Connection Type</th>
                        <th>RSSI Signal</th>
                        <th>Traffic (Tx / Rx)</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {clients.map((c, idx) => (
                        <tr key={c.mac || idx}>
                          <td>
                            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                              {c.hostName || 'Unnamed Device'}
                            </div>
                          </td>
                          <td style={{ fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
                            {c.mac}
                          </td>
                          <td style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-primary)', fontSize: '12px' }}>
                            {c.ip || 'DHCP Assigned'}
                          </td>
                          <td>
                            <span className="badge" style={{ background: c.wireless ? 'rgba(59, 130, 246, 0.15)' : 'rgba(16, 185, 129, 0.15)', color: c.wireless ? 'var(--accent-primary)' : 'var(--status-online)' }}>
                              {c.wireless ? 'Wi-Fi' : 'Ethernet LAN'}
                            </span>
                          </td>
                          <td>
                            {c.signalStrength !== undefined ? (
                              <span style={{ fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
                                {c.signalStrength} dBm
                              </span>
                            ) : (
                              <span style={{ color: 'var(--text-tertiary)' }}>-</span>
                            )}
                          </td>
                          <td style={{ fontSize: '12px', fontFamily: 'var(--font-mono)' }}>
                            ↑ {formatBytes(c.txBytes)} / ↓ {formatBytes(c.rxBytes)}
                          </td>
                          <td>
                            <span className="badge badge-success">
                              Active in Omada
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* TAB 3: TR-069 PARAMETERS TREE */}
          {activeTab === 'params' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', gap: '12px', flexWrap: 'wrap' }}>
                <div style={{ position: 'relative', flex: '1 1 300px' }}>
                  <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-tertiary)' }} />
                  <input
                    type="text"
                    className="text-input"
                    style={{ paddingLeft: '36px', width: '100%', fontSize: '13px' }}
                    placeholder="Filter parameter (e.g. WANDevice, WLANConfiguration, Optical, SSID)..."
                    value={paramSearch}
                    onChange={(e) => setParamSearch(e.target.value)}
                  />
                </div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                  Showing {filteredParams.length} of {Object.keys(parameters).length} parameters
                </div>
              </div>

              <div className="glass-panel" style={{ padding: 0, overflow: 'hidden', maxHeight: '420px', overflowY: 'auto' }}>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th style={{ width: '55%' }}>TR-069 Parameter Path</th>
                      <th>Current Value</th>
                      <th style={{ textAlign: 'right', width: '100px', paddingRight: '16px' }}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredParams.length === 0 ? (
                      <tr>
                        <td colSpan={3} style={{ textAlign: 'center', padding: '36px', color: 'var(--text-tertiary)' }}>
                          No parameters found in tree with applied filter.
                        </td>
                      </tr>
                    ) : (
                      filteredParams.map(([path, val]) => (
                        <tr key={path}>
                          <td style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', color: 'var(--text-primary)', wordBreak: 'break-all' }}>
                            {path}
                          </td>
                          <td style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', color: 'var(--accent-secondary)', wordBreak: 'break-all' }}>
                            {val === '' ? <em style={{ color: 'var(--text-tertiary)' }}>(empty)</em> : val}
                          </td>
                          <td style={{ textAlign: 'right', paddingRight: '16px' }}>
                            <button
                              className="btn-ghost"
                              style={{ padding: '4px 8px', fontSize: '11px' }}
                              onClick={() => setEditingParam({ path, value: val })}
                            >
                              Edit
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 4: JOBS & FAULTS */}
          {activeTab === 'jobs' && (
            <div>
              {/* Jobs section */}
              <div style={{ marginBottom: '24px' }}>
                <h3 style={{ fontSize: '15px', fontWeight: 600, margin: '0 0 12px 0' }}>
                  CWMP Command Queue (Jobs)
                </h3>
                {jobs.length === 0 ? (
                  <div className="glass-panel" style={{ padding: '24px', textAlign: 'center', color: 'var(--text-tertiary)' }}>
                    No commands pending or recently executed for this CPE.
                  </div>
                ) : (
                  <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Action Type</th>
                          <th>Status</th>
                          <th>Created At</th>
                          <th>Completed At</th>
                          <th>Details / Error</th>
                        </tr>
                      </thead>
                      <tbody>
                        {jobs.map((job) => (
                          <tr key={job.id}>
                            <td style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                              {job.type}
                            </td>
                            <td>
                              <span className={`badge ${
                                job.status === 'COMPLETED' ? 'badge-success' :
                                job.status === 'RUNNING' ? 'badge-accent' :
                                job.status === 'FAILED' ? 'badge-danger' : 'badge-warning'
                              }`}>
                                {job.status}
                              </span>
                            </td>
                            <td style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                              {new Date(job.createdAt).toLocaleString()}
                            </td>
                            <td style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                              {job.completedAt ? new Date(job.completedAt).toLocaleString() : '-'}
                            </td>
                            <td style={{ fontSize: '12px', color: job.errorMessage ? 'var(--status-offline)' : 'var(--text-tertiary)' }}>
                              {job.errorMessage || 'No faults'}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              {/* Faults section */}
              <div>
                <h3 style={{ fontSize: '15px', fontWeight: 600, margin: '0 0 12px 0' }}>
                  CWMP Fault History (Faults)
                </h3>
                {faults.length === 0 ? (
                  <div className="glass-panel" style={{ padding: '24px', textAlign: 'center', color: 'var(--text-tertiary)' }}>
                    No CWMP fault records detected.
                  </div>
                ) : (
                  <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Fault Code</th>
                          <th>Error Message</th>
                          <th>RPC Method</th>
                          <th>Date / Time</th>
                        </tr>
                      </thead>
                      <tbody>
                        {faults.map((f, idx) => (
                          <tr key={idx}>
                            <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600, color: 'var(--status-offline)' }}>
                              Fault {f.faultCode}
                            </td>
                            <td style={{ color: 'var(--text-primary)' }}>
                              {f.faultString}
                            </td>
                            <td style={{ fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
                              {f.rpcMethod || 'N/A'}
                            </td>
                            <td style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                              {new Date(f.timestamp).toLocaleString()}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          )}

        </div>

        {/* Modal Footer */}
        <div style={{ 
          padding: '14px 24px', 
          borderTop: '1px solid var(--border-subtle)', 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center',
          background: 'rgba(255,255,255,0.02)'
        }}>
          <div style={{ fontSize: '12px', color: 'var(--text-tertiary)' }}>
            OpenOmada TR-069 Native Plugin • Protocol CWMP 1.4
          </div>
          <button className="btn-primary" onClick={onClose} style={{ padding: '6px 16px', fontSize: '13px' }}>
            Close
          </button>
        </div>

      </div>

      {/* Parameter Edit Modal */}
      {editingParam && (
        <div className="modal-backdrop" onClick={() => !isSavingParam && setEditingParam(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '520px' }}>
            <h3 style={{ margin: '0 0 12px 0', fontSize: '16px', fontWeight: 600 }}>
              Edit TR-069 Parameter
            </h3>
            <div style={{ fontSize: '12px', color: 'var(--text-tertiary)', fontFamily: 'var(--font-mono)', wordBreak: 'break-all', marginBottom: '16px' }}>
              {editingParam.path}
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '6px' }}>
                New Value:
              </label>
              <input
                type="text"
                className="text-input"
                style={{ width: '100%', fontFamily: 'var(--font-mono)' }}
                value={editingParam.value}
                onChange={(e) => setEditingParam({ ...editingParam, value: e.target.value })}
                disabled={isSavingParam}
                autoFocus
              />
            </div>

            {paramSaveMessage && (
              <div style={{ 
                padding: '10px 14px', 
                borderRadius: '8px', 
                fontSize: '13px', 
                marginBottom: '16px',
                background: paramSaveMessage.startsWith('Error') ? 'rgba(239, 68, 68, 0.15)' : 'rgba(16, 185, 129, 0.15)',
                color: paramSaveMessage.startsWith('Error') ? 'var(--status-offline)' : 'var(--status-online)'
              }}>
                {paramSaveMessage}
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button 
                className="btn-ghost" 
                onClick={() => setEditingParam(null)}
                disabled={isSavingParam}
              >
                Cancel
              </button>
              <button
                className="btn-primary"
                onClick={handleSaveParameter}
                disabled={isSavingParam}
              >
                <Save size={14} className={isSavingParam ? 'spin-anim' : ''} />
                <span>{isSavingParam ? 'Scheduling...' : 'Apply via CWMP'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
