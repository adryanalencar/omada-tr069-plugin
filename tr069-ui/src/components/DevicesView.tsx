import React, { useState, useMemo } from 'react';
import { 
  Tr069Device,
  isDeviceOnline
} from '../types/tr069';
import { 
  Search, 
  RefreshCw, 
  RotateCcw, 
  AlertTriangle, 
  Sliders, 
  Radio, 
  Wifi, 
  Layers, 
  ExternalLink,
  ChevronRight,
  Filter,
  Activity
} from 'lucide-react';

interface DevicesViewProps {
  devices: Tr069Device[];
  loading: boolean;
  onRefresh: () => void;
  onSelectDevice: (device: Tr069Device) => void;
  onTriggerConnectionRequest: (mac: string) => Promise<void>;
  onReboot: (mac: string) => Promise<void>;
  onFactoryReset: (mac: string) => Promise<void>;
}

export const DevicesView: React.FC<DevicesViewProps> = ({
  devices,
  loading,
  onRefresh,
  onSelectDevice,
  onTriggerConnectionRequest,
  onReboot,
  onFactoryReset,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ONLINE' | 'OFFLINE'>('ALL');
  const [actionLoading, setActionLoading] = useState<Record<string, string>>({});
  const [confirmModal, setConfirmModal] = useState<{
    mac: string;
    action: 'reboot' | 'reset';
    model?: string;
  } | null>(null);

  const filteredDevices = useMemo(() => {
    return devices.filter((d) => {
      // Status filter
      if (statusFilter === 'ONLINE' && !isDeviceOnline(d)) return false;
      if (statusFilter === 'OFFLINE' && isDeviceOnline(d)) return false;

      // Text search
      if (!searchTerm) return true;
      const term = searchTerm.toLowerCase();
      return (
        (d.mac && d.mac.toLowerCase().includes(term)) ||
        (d.ip && d.ip.toLowerCase().includes(term)) ||
        (d.modelName && d.modelName.toLowerCase().includes(term)) ||
        (d.manufacturer && d.manufacturer.toLowerCase().includes(term)) ||
        (d.serialNumber && d.serialNumber.toLowerCase().includes(term)) ||
        (d.wlanSsid && d.wlanSsid.toLowerCase().includes(term))
      );
    });
  }, [devices, searchTerm, statusFilter]);

  const handleAction = async (mac: string, actionType: 'cr' | 'reboot' | 'reset') => {
    setActionLoading((prev) => ({ ...prev, [mac]: actionType }));
    try {
      if (actionType === 'cr') {
        await onTriggerConnectionRequest(mac);
      } else if (actionType === 'reboot') {
        await onReboot(mac);
      } else if (actionType === 'reset') {
        await onFactoryReset(mac);
      }
    } finally {
      setActionLoading((prev) => {
        const next = { ...prev };
        delete next[mac];
        return next;
      });
      setConfirmModal(null);
    }
  };

  const formatRelativeTime = (timestamp?: number) => {
    if (!timestamp) return 'Never';
    const diff = Math.floor((Date.now() - timestamp) / 1000);
    if (diff < 15) return 'just now';
    if (diff < 60) return `${diff}s ago`;
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  };

  return (
    <div className="view-container">
      {/* Header controls bar */}
      <div className="glass-panel" style={{ padding: '16px 20px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
          {/* Search Box */}
          <div style={{ position: 'relative', flex: '1 1 320px', maxWidth: '480px' }}>
            <Search 
              size={18} 
              style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-tertiary)' }} 
            />
            <input
              type="text"
              className="text-input"
              style={{ paddingLeft: '38px', width: '100%' }}
              placeholder="Search by MAC, IP, Model, Serial, SSID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          {/* Status Filters */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '13px', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '4px' }}>
              <Filter size={14} /> Filter:
            </span>
            <div style={{ display: 'flex', background: 'var(--surface-tertiary)', padding: '3px', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
              <button
                className={`tab-pill ${statusFilter === 'ALL' ? 'active' : ''}`}
                onClick={() => setStatusFilter('ALL')}
              >
                All ({devices.length})
              </button>
              <button
                className={`tab-pill ${statusFilter === 'ONLINE' ? 'active' : ''}`}
                onClick={() => setStatusFilter('ONLINE')}
              >
                Online ({devices.filter(isDeviceOnline).length})
              </button>
              <button
                className={`tab-pill ${statusFilter === 'OFFLINE' ? 'active' : ''}`}
                onClick={() => setStatusFilter('OFFLINE')}
              >
                Offline ({devices.filter(d => !isDeviceOnline(d)).length})
              </button>
            </div>

            <button 
              className="action-btn"
              onClick={onRefresh}
              title="Refresh list"
              disabled={loading}
              style={{ marginLeft: '4px' }}
            >
              <RefreshCw size={15} className={loading ? 'spin-anim' : ''} />
            </button>
          </div>
        </div>
      </div>

      {/* Devices Table */}
      <div className="glass-panel" style={{ padding: '0', overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th style={{ width: '40px', textAlign: 'center' }}>Status</th>
                <th>Device / Manufacturer</th>
                <th>Identification (MAC / Serial)</th>
                <th>IP Address & Management</th>
                <th>Wi-Fi / Stations</th>
                <th>Last Inform</th>
                <th style={{ textAlign: 'right', paddingRight: '20px' }}>Quick Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredDevices.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-tertiary)' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                      <Radio size={40} style={{ opacity: 0.4 }} />
                      <div style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-secondary)' }}>
                        No CPE found
                      </div>
                      <div style={{ fontSize: '13px', maxWidth: '400px', lineHeight: 1.5 }}>
                        {searchTerm 
                          ? 'No devices match the applied search criteria.' 
                          : 'Waiting for the first Inform from the CPE device to the ACS on /tr069.'}
                      </div>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredDevices.map((d) => {
                  const isOnline = isDeviceOnline(d);
                  const clientsCount = (d.connectedClients?.length) || (d.wlanClientsCount || 0) + (d.lanClientsCount || 0);

                  return (
                    <tr 
                      key={d.mac}
                      style={{ cursor: 'pointer' }}
                      onClick={() => onSelectDevice(d)}
                    >
                      {/* Status */}
                      <td style={{ textAlign: 'center' }} onClick={(e) => e.stopPropagation()}>
                        <span 
                          className={`status-indicator ${isOnline ? 'online' : 'offline'}`}
                          title={isOnline ? 'Online (Active in Omada)' : 'Offline'}
                        />
                      </td>

                      {/* Device & Vendor */}
                      <td>
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                          {d.modelName || 'TR-069 CPE'}
                          {d.productClass && (
                            <span style={{ fontSize: '10px', background: 'rgba(255,255,255,0.08)', padding: '1px 6px', borderRadius: '4px', color: 'var(--text-tertiary)' }}>
                              {d.productClass}
                            </span>
                          )}
                        </div>
                        <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '2px' }}>
                          {d.manufacturer || 'Unknown Manufacturer'} {d.softwareVersion ? `• v${d.softwareVersion}` : ''}
                        </div>
                      </td>

                      {/* MAC & Serial */}
                      <td>
                        <div style={{ fontFamily: 'var(--font-mono)', fontSize: '13px', color: 'var(--text-primary)', letterSpacing: '0.5px' }}>
                          {d.mac}
                        </div>
                        <div style={{ fontSize: '11px', color: 'var(--text-tertiary)', fontFamily: 'var(--font-mono)', marginTop: '2px' }}>
                          SN: {d.serialNumber || 'N/A'}
                        </div>
                      </td>

                      {/* IP & Connection Request URL */}
                      <td>
                        <div style={{ fontFamily: 'var(--font-mono)', fontSize: '13px', color: 'var(--accent-primary)', fontWeight: 500 }}>
                          {d.ip || '0.0.0.0'}
                        </div>
                        <div style={{ fontSize: '11px', color: 'var(--text-tertiary)', marginTop: '2px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                          {d.connectionRequestUrl ? (
                            <span title={d.connectionRequestUrl} style={{ textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap', maxWidth: '180px' }}>
                              CR: Active ({new URL(d.connectionRequestUrl).port || '7547'})
                            </span>
                          ) : (
                            <span>CR: Not configured</span>
                          )}
                        </div>
                      </td>

                      {/* Wi-Fi & Clients */}
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <span className="badge badge-accent" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                            <Wifi size={11} /> {clientsCount} {clientsCount === 1 ? 'client' : 'clients'}
                          </span>
                        </div>
                        {d.wlanSsid && (
                          <div style={{ fontSize: '11px', color: 'var(--text-secondary)', marginTop: '3px' }}>
                            SSID: <span style={{ color: 'var(--text-primary)' }}>{d.wlanSsid}</span>
                          </div>
                        )}
                      </td>

                      {/* Last Inform */}
                      <td>
                        <div style={{ fontSize: '13px', color: isOnline ? 'var(--status-online)' : 'var(--text-tertiary)', fontWeight: 500 }}>
                          {formatRelativeTime(d.lastInformTime)}
                        </div>
                        <div style={{ fontSize: '11px', color: 'var(--text-tertiary)', marginTop: '2px' }}>
                          {d.lastInformTime ? new Date(d.lastInformTime).toLocaleTimeString() : 'N/A'}
                        </div>
                      </td>

                      {/* Actions */}
                      <td style={{ textAlign: 'right', paddingRight: '20px' }} onClick={(e) => e.stopPropagation()}>
                        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                          {/* Connection Request Trigger */}
                          <button
                            className="btn-ghost"
                            style={{ padding: '6px 10px', fontSize: '12px' }}
                            title="Send Connection Request (Wake CPE)"
                            disabled={!d.connectionRequestUrl || actionLoading[d.mac] === 'cr'}
                            onClick={() => handleAction(d.mac, 'cr')}
                          >
                            <Radio size={14} className={actionLoading[d.mac] === 'cr' ? 'spin-anim' : ''} />
                            <span>Wake</span>
                          </button>

                          {/* Reboot Button */}
                          <button
                            className="btn-ghost"
                            style={{ padding: '6px 8px' }}
                            title="Reboot CPE remotely via CWMP"
                            disabled={actionLoading[d.mac] === 'reboot'}
                            onClick={() => setConfirmModal({ mac: d.mac, action: 'reboot', model: d.modelName })}
                          >
                            <RotateCcw size={14} className={actionLoading[d.mac] === 'reboot' ? 'spin-anim' : ''} />
                          </button>

                          {/* Details Button */}
                          <button
                            className="btn-outline"
                            style={{ padding: '6px 12px', fontSize: '12px' }}
                            onClick={() => onSelectDevice(d)}
                          >
                            <span>Details</span>
                            <ChevronRight size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Confirmation Modal */}
      {confirmModal && (
        <div className="modal-backdrop" onClick={() => setConfirmModal(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '440px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
              <div style={{ 
                width: '40px', 
                height: '40px', 
                borderRadius: '10px', 
                background: confirmModal.action === 'reset' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: confirmModal.action === 'reset' ? 'var(--status-offline)' : 'var(--status-warning)'
              }}>
                <AlertTriangle size={22} />
              </div>
              <div>
                <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 600 }}>
                  {confirmModal.action === 'reboot' ? 'Reboot Device' : 'Factory Reset'}
                </h3>
                <div style={{ fontSize: '12px', color: 'var(--text-tertiary)', marginTop: '2px' }}>
                  {confirmModal.model || 'TR-069 CPE'} • {confirmModal.mac}
                </div>
              </div>
            </div>

            <p style={{ fontSize: '14px', color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '24px' }}>
              {confirmModal.action === 'reboot'
                ? 'Are you sure you want to send the Reboot CWMP RPC command? The CPE will reboot and reconnect in approximately 60 seconds.'
                : 'WARNING: The Factory Reset command will restore the manufacturer default settings. All WAN, Wi-Fi, and VLAN credentials will be erased!'}
            </p>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button 
                className="btn-ghost" 
                onClick={() => setConfirmModal(null)}
              >
                Cancel
              </button>
              <button
                className={confirmModal.action === 'reset' ? 'btn-danger' : 'btn-primary'}
                onClick={() => handleAction(confirmModal.mac, confirmModal.action)}
                disabled={!!actionLoading[confirmModal.mac]}
              >
                {actionLoading[confirmModal.mac] ? 'Sending...' : (confirmModal.action === 'reboot' ? 'Confirm Reboot' : 'Execute Reset')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
