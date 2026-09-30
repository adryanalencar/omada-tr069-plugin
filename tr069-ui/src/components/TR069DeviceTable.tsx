import React, { useState, useMemo } from 'react';
import { Tr069Device, isDeviceOnline } from '../types/tr069';
import { OmadaFilterBar } from './OmadaFilterBar';
import { OmadaTable, Column } from './OmadaTable';
import { OmadaButton } from './OmadaButton';
import { OmadaStatusDot } from './OmadaStatusDot';
import { 
  DeviceGatewayIcon, 
  DeviceApIcon, 
  LocateIcon, 
  PowerIcon, 
  EditIcon, 
  ExportIcon 
} from './OmadaIcons';
import { Wifi, Radio, RotateCcw, AlertTriangle } from 'lucide-react';

interface TR069DeviceTableProps {
  devices: Tr069Device[];
  onSelectDevice: (device: Tr069Device) => void;
  onTriggerConnectionRequest: (mac: string) => Promise<void>;
  onReboot: (mac: string) => Promise<void>;
}

export const TR069DeviceTable: React.FC<TR069DeviceTableProps> = ({
  devices,
  onSelectDevice,
  onTriggerConnectionRequest,
  onReboot,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [selectedStatus, setSelectedStatus] = useState('all');
  const [currentPage, setCurrentPage] = useState(1);
  const [actionLoading, setActionLoading] = useState<Record<string, string>>({});
  const [confirmModal, setConfirmModal] = useState<{
    mac: string;
    model?: string;
  } | null>(null);

  const filteredDevices = useMemo(() => {
    return devices.filter((d) => {
      // Text search
      if (searchTerm) {
        const term = searchTerm.toLowerCase();
        const matches =
          (d.mac && d.mac.toLowerCase().includes(term)) ||
          (d.ip && d.ip.toLowerCase().includes(term)) ||
          (d.modelName && d.modelName.toLowerCase().includes(term)) ||
          (d.manufacturer && d.manufacturer.toLowerCase().includes(term)) ||
          (d.serialNumber && d.serialNumber.toLowerCase().includes(term)) ||
          (d.wlanSsid && d.wlanSsid.toLowerCase().includes(term));
        if (!matches) return false;
      }

      // Category filter
      const isGpon = (d.modelName && d.modelName.toUpperCase().includes('F6')) || 
                     (d.modelName && d.modelName.toUpperCase().includes('HG')) ||
                     d.productClass === 'GPON';
      if (selectedCategory === 'gpon' && !isGpon) return false;
      if (selectedCategory === 'wifi' && isGpon) return false;

      // Status filter
      if (selectedStatus === 'online' && !isDeviceOnline(d)) return false;
      if (selectedStatus === 'offline' && isDeviceOnline(d)) return false;

      return true;
    });
  }, [devices, searchTerm, selectedCategory, selectedStatus]);

  const handleAction = async (mac: string, actionType: 'cr' | 'reboot') => {
    setActionLoading((prev) => ({ ...prev, [mac]: actionType }));
    try {
      if (actionType === 'cr') {
        await onTriggerConnectionRequest(mac);
      } else if (actionType === 'reboot') {
        await onReboot(mac);
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

  const handleExportCSV = () => {
    const headers = ['MAC', 'Model', 'Manufacturer', 'Serial', 'IP', 'Status', 'SSID', 'Clients', 'Last_Inform'];
    const rows = filteredDevices.map(d => [
      d.mac,
      d.modelName || '',
      d.manufacturer || '',
      d.serialNumber || '',
      d.ip || '',
      isDeviceOnline(d) ? 'Online' : 'Offline',
      d.wlanSsid || '',
      d.connectedClients?.length || 0,
      d.lastInformTime ? new Date(d.lastInformTime).toISOString() : ''
    ]);
    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `tr069_devices_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const gponCount = devices.filter(d => 
    (d.modelName && d.modelName.toUpperCase().includes('F6')) || 
    (d.modelName && d.modelName.toUpperCase().includes('HG')) ||
    d.productClass === 'GPON'
  ).length;
  const wifiCount = devices.length - gponCount;
  const onlineCount = devices.filter(isDeviceOnline).length;
  const offlineCount = devices.length - onlineCount;

  const columns: Column<Tr069Device>[] = [
    {
      key: 'modelName',
      title: 'Device / Model',
      sortable: true,
      render: (d) => {
        const isGpon = (d.modelName && d.modelName.toUpperCase().includes('F6')) || 
                       (d.modelName && d.modelName.toUpperCase().includes('HG')) ||
                       d.productClass === 'GPON';
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            {isGpon ? <DeviceGatewayIcon size={24} /> : <DeviceApIcon size={24} />}
            <div>
              <div style={{ fontWeight: 600, color: '#2B2B2B', fontSize: '13px' }}>
                {d.modelName || 'TR-069 CPE'}
              </div>
              <div style={{ fontSize: '11px', color: '#999999', fontFamily: 'var(--font-mono)' }}>
                SN: {d.serialNumber || 'N/A'} • {d.manufacturer || 'Manufacturer'}
              </div>
            </div>
          </div>
        );
      },
    },
    {
      key: 'mac',
      title: 'MAC Address',
      render: (d) => (
        <span style={{ fontFamily: 'var(--font-mono)', color: '#2B2B2B', fontSize: '12px', fontWeight: 500 }}>
          {d.mac}
        </span>
      ),
    },
    {
      key: 'ip',
      title: 'IP Address / WAN',
      render: (d) => (
        <div>
          <div style={{ fontFamily: 'var(--font-mono)', color: '#0069CB', fontSize: '12px', fontWeight: 500 }}>
            {d.ip || '0.0.0.0'}
          </div>
          <div style={{ fontSize: '11px', color: '#636363' }}>
            CR: {d.connectionRequestUrl ? 'Port 7547' : 'Not configured'}
          </div>
        </div>
      ),
    },
    {
      key: 'optical',
      title: 'Optical Power (RX)',
      render: (d) => {
        const isGpon = (d.modelName && d.modelName.toUpperCase().includes('F6')) || 
                       (d.modelName && d.modelName.toUpperCase().includes('HG')) ||
                       d.productClass === 'GPON' ||
                       d.rxOpticalPowerDbm != null;
        if (!isGpon || d.rxOpticalPowerDbm == null || isNaN(d.rxOpticalPowerDbm)) {
          return <span style={{ color: '#CCCCCC' }}>—</span>;
        }
        const rxVal = d.rxOpticalPowerDbm > 0 ? -d.rxOpticalPowerDbm : d.rxOpticalPowerDbm;
        const isGood = rxVal >= -27 && rxVal <= -8;
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontWeight: 600, color: isGood ? '#05C178' : '#EE385C' }}>
              {rxVal.toFixed(2)} dBm
            </span>
            {d.txOpticalPowerDbm != null && (
              <span style={{ fontSize: '11px', color: '#999999' }}>
                (TX: +{Math.abs(d.txOpticalPowerDbm).toFixed(2)} dBm)
              </span>
            )}
          </div>
        );
      },
    },
    {
      key: 'wlan',
      title: 'Wi-Fi & Clients',
      render: (d) => {
        const count = d.connectedClients?.length || (d.wlanClientsCount || 0) + (d.lanClientsCount || 0);
        return (
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
              <span className="omada-badge omada-badge-info" style={{ fontSize: '11px' }}>
                <Wifi size={11} /> {count} {count === 1 ? 'client' : 'clients'}
              </span>
            </div>
            {d.wlanSsid && (
              <div style={{ fontSize: '11px', color: '#636363', marginTop: '3px' }}>
                SSID: <strong style={{ color: '#2B2B2B' }}>{d.wlanSsid}</strong>
              </div>
            )}
          </div>
        );
      },
    },
    {
      key: 'softwareVersion',
      title: 'Firmware',
      render: (d) => (
        <span style={{ color: '#636363', fontSize: '12px', fontFamily: 'var(--font-mono)' }}>
          {d.softwareVersion || 'V9.0'}
        </span>
      ),
    },
    {
      key: 'status',
      title: 'Status',
      render: (d) => {
        const online = isDeviceOnline(d);
        let timeAgo = '';
        if (d.lastSeen) {
          const sec = Math.max(0, Math.round((Date.now() - d.lastSeen) / 1000));
          if (sec < 60) timeAgo = `${sec}s ago`;
          else if (sec < 3600) timeAgo = `${Math.floor(sec / 60)}m ago`;
          else timeAgo = `${Math.floor(sec / 3600)}h ago`;
        }
        return (
          <div>
            <OmadaStatusDot status={online ? 'Online' : 'Offline'} />
            {timeAgo && (
              <div style={{ fontSize: '10px', color: '#888888', marginTop: '2px', fontFamily: 'monospace' }}>
                {timeAgo}
              </div>
            )}
          </div>
        );
      },
    },
    {
      key: 'actions',
      title: 'Actions',
      align: 'right',
      render: (d) => (
        <div style={{ display: 'inline-flex', gap: '14px', alignItems: 'center' }} onClick={(e) => e.stopPropagation()}>
          <span 
            title="Send Connection Request (Wake CPE)"
            style={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}
            onClick={() => handleAction(d.mac, 'cr')}
          >
            <LocateIcon 
              size={16} 
              color="#00A870" 
              className={actionLoading[d.mac] === 'cr' ? 'spin-anim' : ''} 
            />
          </span>

          <span 
            title="Reboot Device Remotely via RPC"
            style={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}
            onClick={() => setConfirmModal({ mac: d.mac, model: d.modelName })}
          >
            <PowerIcon 
              size={16} 
              color="#EE385C" 
              className={actionLoading[d.mac] === 'reboot' ? 'spin-anim' : ''} 
            />
          </span>

          <span 
            title="Inspect TR-181 Parameters & Telemetry"
            style={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}
            onClick={() => onSelectDevice(d)}
          >
            <EditIcon size={16} color="#00A870" />
          </span>
        </div>
      ),
    },
  ];

  return (
    <div>
      {/* Native Omada Filter & Action Bar */}
      <OmadaFilterBar
        searchValue={searchTerm}
        onSearchChange={setSearchTerm}
        searchPlaceholder="Search by Name, Serial, MAC, IP or SSID..."
        pills={[
          { id: 'all', label: 'All', count: devices.length },
          { id: 'gpon', label: 'GPON ONTs', count: gponCount },
          { id: 'wifi', label: 'Wi-Fi Routers', count: wifiCount },
        ]}
        activePill={selectedCategory}
        onPillChange={setSelectedCategory}
        statusPills={[
          { id: 'all', label: 'All', count: devices.length },
          { id: 'online', label: 'Online', count: onlineCount, dotColor: '#05C178' },
          { id: 'offline', label: 'Offline', count: offlineCount, dotColor: '#EE385C' },
        ]}
        activeStatusPill={selectedStatus}
        onStatusPillChange={setSelectedStatus}
        rightActions={
          <>
            <OmadaButton variant="link" icon={<ExportIcon size={14} />} onClick={handleExportCSV}>
              Export CSV
            </OmadaButton>
          </>
        }
      />

      {/* Native Omada Table */}
      <div style={{ background: '#FFFFFF', borderRadius: '8px', border: '1px solid var(--omada-border, #ECECEC)', overflow: 'hidden' }}>
        <OmadaTable
          columns={columns}
          data={filteredDevices}
          rowKey={(d) => d.mac}
          totalRecords={filteredDevices.length}
          currentPage={currentPage}
          onPageChange={setCurrentPage}
          onRowClick={(d) => onSelectDevice(d)}
        />
      </div>

      {/* Confirmation Modal for Reboot */}
      {confirmModal && (
        <div className="omada-modal-overlay" onClick={() => setConfirmModal(null)}>
          <div className="omada-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '440px' }}>
            <div className="omada-modal-header">
              <h2 className="omada-modal-title" style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#EE385C' }}>
                <AlertTriangle size={18} /> Confirm Reboot
              </h2>
              <button className="omada-modal-close" onClick={() => setConfirmModal(null)}>✕</button>
            </div>
            <div className="omada-modal-body" style={{ padding: '20px' }}>
              <p style={{ fontSize: '13px', color: '#2B2B2B', lineHeight: 1.5 }}>
                Do you want to send the <strong>Reboot</strong> CWMP RPC command to device{' '}
                <strong>{confirmModal.model || 'CPE'}</strong> ({confirmModal.mac})?
              </p>
              <p style={{ fontSize: '12px', color: '#636363', marginTop: '8px' }}>
                The device will reboot physically and reconnect to Omada shortly.
              </p>
            </div>
            <div className="omada-modal-footer">
              <OmadaButton variant="default" onClick={() => setConfirmModal(null)}>
                Cancel
              </OmadaButton>
              <OmadaButton 
                variant="danger" 
                onClick={() => handleAction(confirmModal.mac, 'reboot')}
                disabled={actionLoading[confirmModal.mac] === 'reboot'}
              >
                {actionLoading[confirmModal.mac] === 'reboot' ? 'Sending...' : 'Confirm Reboot'}
              </OmadaButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
