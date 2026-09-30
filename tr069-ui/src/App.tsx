import React, { useState, useEffect, useCallback } from 'react';
import { Tr069Device, Tr069Metrics, isDeviceOnline } from './types/tr069';
import { tr069Api } from './services/api';
import { OmadaHeader } from './components/OmadaHeader';
import { OmadaTabs } from './components/OmadaTabs';
import { TR069SummarySection } from './components/TR069SummarySection';
import { TR069DeviceTable } from './components/TR069DeviceTable';
import { TR069DeviceInspectorModal } from './components/TR069DeviceInspectorModal';
import { TR069ServerConfig } from './components/TR069ServerConfig';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'devices' | 'server'>('devices');
  const [devices, setDevices] = useState<Tr069Device[]>([]);
  const [metrics, setMetrics] = useState<Tr069Metrics | null>(null);
  const [loading, setLoading] = useState(false);
  const [selectedDevice, setSelectedDevice] = useState<Tr069Device | null>(null);
  const [refreshInterval, setRefreshInterval] = useState<number>(10);
  const [toastMessage, setToastMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const showToast = (text: string, type: 'success' | 'error' = 'success') => {
    setToastMessage({ text, type });
    setTimeout(() => setToastMessage(null), 3500);
  };

  const loadData = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const [devResult, metResult] = await Promise.all([
        tr069Api.listDevices().catch(() => ({ total: 0, data: [] })),
        tr069Api.getMetrics().catch(() => null),
      ]);
      setDevices(devResult.data || []);
      setMetrics(metResult);
    } catch (err: any) {
      console.error('Failed to load TR069 data', err);
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Auto refresh timer
  useEffect(() => {
    if (!refreshInterval || refreshInterval <= 0) return;
    const interval = setInterval(() => {
      loadData(true);
    }, refreshInterval * 1000);
    return () => clearInterval(interval);
  }, [refreshInterval, loadData]);

  const handleTriggerConnectionRequest = async (mac: string) => {
    try {
      const res = await tr069Api.triggerConnectionRequest(mac);
      showToast(res.message || 'Connection Request sent successfully!');
      await loadData(true);
    } catch (err: any) {
      showToast(`Connection Request failed: ${err.message}`, 'error');
    }
  };

  const handleReboot = async (mac: string) => {
    try {
      await tr069Api.reboot(mac);
      showToast('Reboot command scheduled via CWMP!');
      await loadData(true);
    } catch (err: any) {
      showToast(`Failed to reboot: ${err.message}`, 'error');
    }
  };

  const onlineCount = devices.filter(isDeviceOnline).length;

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* 1. Header matching TP-Link Omada layout */}
      <OmadaHeader
        onlineCount={onlineCount}
        totalCount={devices.length}
        refreshInterval={refreshInterval}
        onRefreshIntervalChange={setRefreshInterval}
        onManualRefresh={() => loadData(false)}
        loading={loading}
      />

      {/* 2. Main Omada Content Area */}
      <main className="omada-content-area">
        {/* Navigation Tabs */}
        <OmadaTabs
          tabs={[
            { id: 'devices', label: 'CPE Devices & ONTs', badge: devices.length },
            { id: 'server', label: 'ACS Server & Vendor Guides' },
          ]}
          activeTab={activeTab}
          onChange={(tab) => setActiveTab(tab as any)}
          showHelp={false}
        />

        {/* Tab 1: Devices */}
        {activeTab === 'devices' && (
          <>
            <TR069SummarySection devices={devices} metrics={metrics} />
            <TR069DeviceTable
              devices={devices}
              onSelectDevice={(d) => setSelectedDevice(d)}
              onTriggerConnectionRequest={handleTriggerConnectionRequest}
              onReboot={handleReboot}
            />
          </>
        )}

        {/* Tab 2: Server Settings & Vendor Guides */}
        {activeTab === 'server' && (
          <TR069ServerConfig />
        )}
      </main>

      {/* 3. Device Detail Inspector Modal */}
      {selectedDevice && (
        <TR069DeviceInspectorModal
          device={selectedDevice}
          onClose={() => setSelectedDevice(null)}
          onTriggerConnectionRequest={handleTriggerConnectionRequest}
          onReboot={handleReboot}
        />
      )}

      {/* 4. Global Toast Notification */}
      {toastMessage && (
        <div style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          background: toastMessage.type === 'error' ? '#EE385C' : '#00A870',
          color: '#FFFFFF',
          padding: '12px 20px',
          borderRadius: '6px',
          fontSize: '13px',
          fontWeight: 600,
          boxShadow: '0 4px 14px rgba(0,0,0,0.15)',
          zIndex: 9999,
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          animation: 'omadaFadeIn 0.2s ease',
        }}>
          {toastMessage.text}
        </div>
      )}
    </div>
  );
};

export default App;
