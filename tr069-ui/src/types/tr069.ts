export interface Tr069Device {
  mac: string;
  manufacturer?: string;
  modelName?: string;
  serialNumber?: string;
  softwareVersion?: string;
  hardwareVersion?: string;
  oui?: string;
  productClass?: string;
  provisioningCode?: string;
  ip?: string;
  status?: string | number; // 1 = ONLINE, 0 = OFFLINE, or "CONNECTED", "ONLINE", "OFFLINE"
  statusCode?: number;
  uptime?: number;
  uptimeSeconds?: number;
  lastSeen?: number;
  lastInformTime?: number;
  periodicInformInterval?: number;
  connectionRequestUrl?: string;
  connectionRequestUsername?: string;
  siteId?: string;
  omadacId?: string;
  rxOpticalPowerDbm?: number;
  txOpticalPowerDbm?: number;
  cpuUsage?: number;
  memoryUsage?: number;
  clientCount?: number;
  wlanSsid?: string;
  wlanChannel?: number;
  wlanClientsCount?: number;
  lanClientsCount?: number;
  connectedClients?: Tr069ConnectedClient[];
}

export const isDeviceOnline = (d: Tr069Device): boolean => {
  if (d.statusCode === 1 || d.status === 1 || d.status === '1') return true;
  if (typeof d.status === 'string') {
    const s = d.status.toUpperCase();
    if (s === 'CONNECTED' || s === 'ONLINE') {
      if (d.lastSeen) {
        const now = Date.now();
        const maxAgeMs = Math.max((d.periodicInformInterval || 60) * 3000, 180000);
        return (now - d.lastSeen) < maxAgeMs;
      }
      return true;
    }
  }
  return false;
};

export interface Tr069Metrics {
  totalInforms: number;
  bootstrapEvents: number;
  periodicEvents: number;
  totalRpcSent: number;
  totalRpcSuccess: number;
  totalRpcFaults: number;
  activeSessions: number;
  totalDevices: number;
}

export interface Tr069Telemetry {
  mac: string;
  timestamp: number;
  uptimeSeconds?: number;
  rxOpticalPowerDbm?: number;
  txOpticalPowerDbm?: number;
  cpuUsage?: number;
  memoryUsage?: number;
  rxBytes?: number;
  txBytes?: number;
  clientCount?: number;
}

export interface Tr069ConnectedClient {
  mac: string;
  ip?: string;
  hostName?: string;
  wireless?: boolean;
  signalStrength?: number;
  rxBytes?: number;
  txBytes?: number;
  active?: boolean;
  connectTime?: number;
}

export interface Tr069FaultRecord {
  mac: string;
  faultCode: number;
  faultString: string;
  rpcMethod?: string;
  timestamp: number;
}

export interface Tr069Job {
  id: string;
  mac: string;
  type: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  createdAt: number;
  completedAt?: number;
  errorMessage?: string;
}
