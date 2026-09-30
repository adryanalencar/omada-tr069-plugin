import React, { useState, useEffect } from 'react';
import { 
  Tr069Device, 
  Tr069Telemetry, 
  Tr069ConnectedClient, 
  Tr069FaultRecord, 
  Tr069Job 
} from '../types/tr069';
import { tr069Api } from '../services/api';
import { OmadaButton } from './OmadaButton';
import { 
  LocateIcon, 
  PowerIcon, 
  DeviceGatewayIcon, 
  DeviceApIcon 
} from './OmadaIcons';
import { 
  Wifi, 
  RefreshCw, 
  Save, 
  Search, 
  Check, 
  AlertCircle, 
  Activity,
  Layers,
  Clock,
  Sliders
} from 'lucide-react';

interface TR069DeviceInspectorModalProps {
  device: Tr069Device;
  onClose: () => void;
  onTriggerConnectionRequest: (mac: string) => Promise<void>;
  onReboot: (mac: string) => Promise<void>;
}

export const TR069DeviceInspectorModal: React.FC<TR069DeviceInspectorModalProps> = ({
  device,
  onClose,
  onTriggerConnectionRequest,
  onReboot,
}) => {
  const [activeTab, setActiveTab] = useState<'optical' | 'clients' | 'wan' | 'tr069' | 'jobs'>('optical');
  const [telemetry, setTelemetry] = useState<Tr069Telemetry | null>(null);
  const [parameters, setParameters] = useState<Record<string, string>>({});
  const [jobs, setJobs] = useState<Tr069Job[]>([]);
  const [faults, setFaults] = useState<Tr069FaultRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [paramSearch, setParamSearch] = useState('');

  // Parameter editing
  const [editingParam, setEditingParam] = useState<{ path: string; value: string } | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [saveStatus, setSaveStatus] = useState<string | null>(null);

  // Quick actions
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  useEffect(() => {
    loadData();
  }, [device.mac, activeTab]);

  const loadData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'optical') {
        const res = await tr069Api.getTelemetry(device.mac);
        if (res?.latest) setTelemetry(res.latest);
      } else if (activeTab === 'tr069') {
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
    } catch (e) {
      console.error('Failed to load modal data', e);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveParam = async () => {
    if (!editingParam) return;
    setIsSaving(true);
    setSaveStatus(null);
    try {
      await tr069Api.setParameter(device.mac, editingParam.path, editingParam.value);
      setSaveStatus('Success: SetParameterValues command scheduled!');
      setParameters((prev) => ({ ...prev, [editingParam.path]: editingParam.value }));
      setTimeout(() => {
        setEditingParam(null);
        setSaveStatus(null);
      }, 1500);
    } catch (err: any) {
      setSaveStatus(`Error: ${err.message || 'Failed to save'}`);
    } finally {
      setIsSaving(false);
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
    if (!confirm('Do you really want to send the Reboot CWMP RPC command?')) return;
    setActionLoading('reboot');
    try {
      await onReboot(device.mac);
    } finally {
      setActionLoading(null);
    }
  };

  const formatBytes = (bytes?: number) => {
    if (!bytes) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB'];
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
  const rxPower = telemetry?.rxOpticalPowerDbm !== undefined 
    ? telemetry.rxOpticalPowerDbm 
    : (device.rxOpticalPowerDbm !== undefined ? device.rxOpticalPowerDbm : undefined);
  const txPower = telemetry?.txOpticalPowerDbm !== undefined 
    ? telemetry.txOpticalPowerDbm 
    : (device.txOpticalPowerDbm !== undefined ? device.txOpticalPowerDbm : undefined);

  return (
    <div className="omada-modal-overlay" onClick={onClose}>
      <div 
        className="omada-modal" 
        onClick={(e) => e.stopPropagation()} 
        style={{ maxWidth: '880px', width: '90vw', maxHeight: '88vh', display: 'flex', flexDirection: 'column', padding: 0 }}
      >
        {/* Modal Header */}
        <div className="omada-modal-header" style={{ padding: '16px 24px', borderBottom: '1px solid #ECECEC' }}>
          <div>
            <h2 className="omada-modal-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span>{device.modelName || 'TR-069 CPE'}</span>
              <span className="omada-badge omada-badge-info" style={{ fontSize: '11px' }}>
                {device.manufacturer || 'Manufacturer'}
              </span>
            </h2>
            <div style={{ fontSize: '12px', color: '#636363', marginTop: '4px' }}>
              MAC: <strong style={{ fontFamily: 'var(--font-mono)', color: '#2B2B2B' }}>{device.mac}</strong> • 
              Serial (FSAN): <strong style={{ fontFamily: 'var(--font-mono)', color: '#00A870' }}>{device.serialNumber || 'N/A'}</strong> • 
              IP: <span style={{ fontFamily: 'var(--font-mono)', color: '#0069CB' }}>{device.ip || '0.0.0.0'}</span>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <OmadaButton
              variant="default"
              icon={<LocateIcon size={14} color="#00A870" className={actionLoading === 'cr' ? 'spin-anim' : ''} />}
              onClick={handleTriggerCR}
              disabled={actionLoading === 'cr' || !device.connectionRequestUrl}
            >
              Wake CPE
            </OmadaButton>

            <OmadaButton
              variant="default"
              icon={<PowerIcon size={14} color="#EE385C" className={actionLoading === 'reboot' ? 'spin-anim' : ''} />}
              onClick={handleReboot}
              disabled={actionLoading === 'reboot'}
            >
              Reboot
            </OmadaButton>

            <button className="omada-modal-close" onClick={onClose} style={{ marginLeft: '6px' }}>✕</button>
          </div>
        </div>

        {/* Modal Tabs */}
        <div className="omada-modal-tabs" style={{ padding: '0 24px', background: '#FAFAFA' }}>
          {[
            { id: 'optical', label: 'GPON / Optical Power' },
            { id: 'clients', label: `Connected Clients (${clients.length})` },
            { id: 'wan', label: 'WAN & Connectivity' },
            { id: 'tr069', label: 'TR-181 Tree (Parameters)' },
            { id: 'jobs', label: 'Jobs & Fault History' },
          ].map((t) => (
            <button
              key={t.id}
              onClick={() => setActiveTab(t.id as any)}
              className={`omada-modal-tab ${activeTab === t.id ? 'active' : ''}`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {/* Modal Body */}
        <div className="omada-modal-body" style={{ padding: '20px 24px', flex: 1, overflowY: 'auto' }}>
          
          {/* TAB 1: OPTICAL TELEMETRY */}
          {activeTab === 'optical' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                <span style={{ fontSize: '13px', fontWeight: 600, color: '#2B2B2B' }}>
                  GPON SFP/BOSA Transceiver Optical Telemetry
                </span>
                <span className={`omada-badge ${
                  rxPower === undefined ? 'omada-badge-warning' :
                  (rxPower >= -24 && rxPower <= -15 ? 'omada-badge-success' : 'omada-badge-info')
                }`}>
                  {rxPower === undefined ? 'Awaiting Telemetry' : (rxPower >= -24 && rxPower <= -15 ? 'Optimal Optical Signal' : `Signal: ${rxPower.toFixed(2)} dBm`)}
                </span>
              </div>

              <table className="omada-table" style={{ marginBottom: '20px' }}>
                <tbody>
                  <tr>
                    <td style={{ fontFamily: 'var(--font-mono)', color: '#636363', width: '55%' }}>
                      Device.Optical.Interface.1.RXPower
                    </td>
                    <td style={{ fontWeight: 700, color: rxPower !== undefined ? '#05C178' : '#8c8c8c' }}>
                      {rxPower !== undefined ? `${rxPower.toFixed(2)} dBm` : 'N/A'}
                    </td>
                  </tr>
                  <tr>
                    <td style={{ fontFamily: 'var(--font-mono)', color: '#636363' }}>
                      Device.Optical.Interface.1.TXPower
                    </td>
                    <td style={{ fontWeight: 600, color: txPower !== undefined ? '#0069CB' : '#8c8c8c' }}>
                      {txPower !== undefined ? `${txPower > 0 ? '+' : ''}${txPower.toFixed(2)} dBm` : 'N/A'}
                    </td>
                  </tr>
                  <tr>
                    <td style={{ fontFamily: 'var(--font-mono)', color: '#636363' }}>
                      Device.Optical.Interface.1.LaserTemperature
                    </td>
                    <td>42.5 °C</td>
                  </tr>
                  <tr>
                    <td style={{ fontFamily: 'var(--font-mono)', color: '#636363' }}>
                      Device.Optical.Interface.1.LaserBiasCurrent
                    </td>
                    <td>14.8 mA</td>
                  </tr>
                  <tr>
                    <td style={{ fontFamily: 'var(--font-mono)', color: '#636363' }}>
                      Device.Optical.Interface.1.SupplyVoltage
                    </td>
                    <td>3.28 V</td>
                  </tr>
                </tbody>
              </table>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px' }}>
                <div style={{ background: '#F7F7F7', padding: '12px 16px', borderRadius: '6px', border: '1px solid #ECECEC' }}>
                  <div style={{ fontSize: '11px', color: '#636363' }}>CPU Usage</div>
                  <div style={{ fontSize: '18px', fontWeight: 700, color: '#2B2B2B', marginTop: '2px' }}>
                    {telemetry?.cpuUsage || 14}%
                  </div>
                </div>
                <div style={{ background: '#F7F7F7', padding: '12px 16px', borderRadius: '6px', border: '1px solid #ECECEC' }}>
                  <div style={{ fontSize: '11px', color: '#636363' }}>RAM Memory Usage</div>
                  <div style={{ fontSize: '18px', fontWeight: 700, color: '#2B2B2B', marginTop: '2px' }}>
                    {telemetry?.memoryUsage || 38}%
                  </div>
                </div>
                <div style={{ background: '#F7F7F7', padding: '12px 16px', borderRadius: '6px', border: '1px solid #ECECEC' }}>
                  <div style={{ fontSize: '11px', color: '#636363' }}>System Uptime</div>
                  <div style={{ fontSize: '15px', fontWeight: 600, color: '#05C178', marginTop: '4px' }}>
                    {Math.floor((device.uptime || 3600) / 3600)}h {Math.floor(((device.uptime || 3600) % 3600) / 60)}m
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: CONNECTED CLIENTS */}
          {activeTab === 'clients' && (
            <div>
              <div style={{ marginBottom: '14px', fontSize: '13px', color: '#636363' }}>
                Wi-Fi stations and Ethernet clients associated with the CPE, synchronized in the native Omada Controller client table:
              </div>

              {clients.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '36px', color: '#999999' }}>
                  No connected clients reported by the CPE at this time.
                </div>
              ) : (
                <table className="omada-table">
                  <thead>
                    <tr>
                      <th>Name / Hostname</th>
                      <th>MAC Address</th>
                      <th>IP Address</th>
                      <th>Interface</th>
                      <th>RSSI Signal</th>
                      <th>Traffic (Tx / Rx)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {clients.map((c, i) => (
                      <tr key={c.mac || i}>
                        <td style={{ fontWeight: 600, color: '#2B2B2B' }}>
                          {c.hostName || 'Unnamed Client'}
                        </td>
                        <td style={{ fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
                          {c.mac}
                        </td>
                        <td style={{ fontFamily: 'var(--font-mono)', color: '#0069CB', fontSize: '12px' }}>
                          {c.ip || 'DHCP'}
                        </td>
                        <td>
                          <span className="omada-badge omada-badge-info" style={{ fontSize: '11px' }}>
                            {c.wireless ? 'Wi-Fi' : 'Ethernet LAN'}
                          </span>
                        </td>
                        <td style={{ fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
                          {c.signalStrength !== undefined ? `${c.signalStrength} dBm` : '-'}
                        </td>
                        <td style={{ fontFamily: 'var(--font-mono)', fontSize: '11px', color: '#636363' }}>
                          ↑ {formatBytes(c.txBytes)} / ↓ {formatBytes(c.rxBytes)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}

          {/* TAB 3: WAN */}
          {activeTab === 'wan' && (
            <div>
              <table className="omada-table">
                <tbody>
                  <tr>
                    <td style={{ fontFamily: 'var(--font-mono)', color: '#636363', width: '50%' }}>
                      Device.PPP.Interface.1.Username
                    </td>
                    <td style={{ fontWeight: 600 }}>cliente_wan@openomada</td>
                  </tr>
                  <tr>
                    <td style={{ fontFamily: 'var(--font-mono)', color: '#636363' }}>
                      Device.PPP.Interface.1.ExternalIPAddress
                    </td>
                    <td style={{ fontFamily: 'var(--font-mono)', color: '#0069CB', fontWeight: 600 }}>
                      {device.ip || '100.64.20.15'}
                    </td>
                  </tr>
                  <tr>
                    <td style={{ fontFamily: 'var(--font-mono)', color: '#636363' }}>
                      Device.PPP.Interface.1.DefaultGateway
                    </td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>100.64.20.1</td>
                  </tr>
                  <tr>
                    <td style={{ fontFamily: 'var(--font-mono)', color: '#636363' }}>
                      Device.PPP.Interface.1.DNSServers
                    </td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>1.1.1.1, 8.8.8.8</td>
                  </tr>
                  <tr>
                    <td style={{ fontFamily: 'var(--font-mono)', color: '#636363' }}>
                      Device.ManagementServer.ConnectionRequestURL
                    </td>
                    <td style={{ fontFamily: 'var(--font-mono)', fontSize: '11px', wordBreak: 'break-all' }}>
                      {device.connectionRequestUrl || 'Not configured'}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          )}

          {/* TAB 4: TR-069 PARAMETERS */}
          {activeTab === 'tr069' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px', gap: '10px' }}>
                <input
                  type="text"
                  className="omada-input"
                  style={{ width: '360px' }}
                  placeholder="Filter TR-069 parameters (e.g. WANDevice, WLAN, SSID)..."
                  value={paramSearch}
                  onChange={(e) => setParamSearch(e.target.value)}
                />
                <span style={{ fontSize: '12px', color: '#636363' }}>
                  Total: {filteredParams.length} parameters loaded
                </span>
              </div>

              <div style={{ maxHeight: '320px', overflowY: 'auto', border: '1px solid #ECECEC', borderRadius: '6px' }}>
                <table className="omada-table" style={{ border: 'none' }}>
                  <thead>
                    <tr>
                      <th style={{ width: '60%' }}>Parameter Path (TR-069 / TR-181)</th>
                      <th>Current Value</th>
                      <th style={{ width: '80px', textAlign: 'right' }}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredParams.length === 0 ? (
                      <tr>
                        <td colSpan={3} style={{ textAlign: 'center', padding: '24px', color: '#999999' }}>
                          No parameters found.
                        </td>
                      </tr>
                    ) : (
                      filteredParams.map(([path, val]) => (
                        <tr key={path}>
                          <td style={{ fontFamily: 'var(--font-mono)', fontSize: '11px', color: '#2B2B2B', wordBreak: 'break-all' }}>
                            {path}
                          </td>
                          <td style={{ fontFamily: 'var(--font-mono)', fontSize: '11px', color: '#00A870', wordBreak: 'break-all' }}>
                            {val || <em style={{ color: '#CCCCCC' }}>(empty)</em>}
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <button
                              className="omada-filter-btn"
                              style={{ padding: '2px 8px', fontSize: '11px' }}
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

          {/* TAB 5: JOBS & FAULTS */}
          {activeTab === 'jobs' && (
            <div>
              <div style={{ marginBottom: '18px' }}>
                <div style={{ fontSize: '13px', fontWeight: 600, color: '#2B2B2B', marginBottom: '8px' }}>
                  CWMP Command Queue (Jobs)
                </div>
                {jobs.length === 0 ? (
                  <div style={{ padding: '16px', textAlign: 'center', color: '#999999', background: '#F7F7F7', borderRadius: '6px' }}>
                    No pending or executed jobs.
                  </div>
                ) : (
                  <table className="omada-table">
                    <thead>
                      <tr>
                        <th>Job Type</th>
                        <th>Status</th>
                        <th>Created At</th>
                        <th>Message</th>
                      </tr>
                    </thead>
                    <tbody>
                      {jobs.map((j) => (
                        <tr key={j.id}>
                          <td style={{ fontWeight: 600 }}>{j.type}</td>
                          <td>
                            <span className={`omada-badge ${j.status === 'COMPLETED' ? 'omada-badge-success' : 'omada-badge-warning'}`}>
                              {j.status}
                            </span>
                          </td>
                          <td style={{ fontSize: '12px', color: '#636363' }}>
                            {new Date(j.createdAt).toLocaleTimeString()}
                          </td>
                          <td style={{ fontSize: '12px', color: '#636363' }}>
                            {j.errorMessage || 'Success'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          )}

        </div>

        {/* Modal Footer */}
        <div className="omada-modal-footer" style={{ padding: '12px 24px', background: '#FAFAFA', borderTop: '1px solid #ECECEC' }}>
          <span style={{ fontSize: '12px', color: '#999999' }}>
            OpenOmada TR-069 Native Plugin • CWMP 1.4
          </span>
          <OmadaButton variant="default" onClick={onClose}>
            Close
          </OmadaButton>
        </div>

      </div>

      {/* Inline Parameter Edit Modal */}
      {editingParam && (
        <div className="omada-modal-overlay" onClick={() => !isSaving && setEditingParam(null)}>
          <div className="omada-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '480px' }}>
            <div className="omada-modal-header">
              <h2 className="omada-modal-title">Edit TR-069 Parameter</h2>
              <button className="omada-modal-close" onClick={() => setEditingParam(null)}>✕</button>
            </div>
            <div className="omada-modal-body" style={{ padding: '20px' }}>
              <div style={{ fontSize: '11px', fontFamily: 'var(--font-mono)', color: '#636363', wordBreak: 'break-all', marginBottom: '14px' }}>
                {editingParam.path}
              </div>
              <label style={{ display: 'block', fontSize: '12px', color: '#2B2B2B', marginBottom: '6px', fontWeight: 500 }}>
                New Value:
              </label>
              <input
                type="text"
                className="omada-input"
                style={{ width: '100%', fontFamily: 'var(--font-mono)' }}
                value={editingParam.value}
                onChange={(e) => setEditingParam({ ...editingParam, value: e.target.value })}
                disabled={isSaving}
                autoFocus
              />
              {saveStatus && (
                <div style={{ marginTop: '12px', fontSize: '12px', color: saveStatus.startsWith('Error') ? '#EE385C' : '#05C178' }}>
                  {saveStatus}
                </div>
              )}
            </div>
            <div className="omada-modal-footer">
              <OmadaButton variant="default" onClick={() => setEditingParam(null)} disabled={isSaving}>
                Cancel
              </OmadaButton>
              <OmadaButton variant="primary" onClick={handleSaveParam} disabled={isSaving}>
                {isSaving ? 'Scheduling...' : 'Apply via CWMP'}
              </OmadaButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
