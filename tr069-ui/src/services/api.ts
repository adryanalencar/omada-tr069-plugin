import { Tr069Device, Tr069Metrics, Tr069Telemetry, Tr069FaultRecord, Tr069Job } from '../types/tr069';

const STORAGE_KEY = 'openomada_acs_host';

export function getAcsHost(): string {
  const custom = localStorage.getItem(STORAGE_KEY);
  if (custom) return custom.replace(/\/+$/, '');
  
  // Default to relative if hosted on Omada server, otherwise localhost/proxy
  if (window.location.port === '5173') {
    return ''; // Vite proxy handles /api/v2/tr069
  }
  return '';
}

export function setAcsHost(host: string): void {
  if (!host) {
    localStorage.removeItem(STORAGE_KEY);
  } else {
    localStorage.setItem(STORAGE_KEY, host.trim());
  }
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const host = getAcsHost();
  const url = `${host}/api/v2/tr069${endpoint}`;
  
  const headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json',
    ...options.headers,
  };

  const response = await fetch(url, { ...options, headers });
  if (!response.ok) {
    let msg = `HTTP ${response.status}: ${response.statusText}`;
    try {
      const errJson = await response.json();
      if (errJson.msg) msg = errJson.msg;
    } catch (_) {}
    throw new Error(msg);
  }

  const data = await response.json();
  if (data.errorCode !== undefined && data.errorCode !== 0) {
    throw new Error(data.msg || 'API error');
  }

  return (data.result !== undefined ? data.result : data) as T;
}

export const tr069Api = {
  // Metrics & Stats
  async getMetrics(): Promise<Tr069Metrics> {
    return request<Tr069Metrics>('/metrics');
  },

  // Devices
  async listDevices(): Promise<{ total: number; data: Tr069Device[] }> {
    return request<{ total: number; data: Tr069Device[] }>('/devices');
  },

  async getDevice(mac: string): Promise<Tr069Device> {
    return request<Tr069Device>(`/devices/${encodeURIComponent(mac)}`);
  },

  // Parameters (Data Model Tree)
  async getParameters(mac: string): Promise<Record<string, string>> {
    return request<Record<string, string>>(`/devices/${encodeURIComponent(mac)}/parameters`);
  },

  async setParameter(mac: string, path: string, value: string): Promise<Tr069Job> {
    return request<Tr069Job>(`/devices/${encodeURIComponent(mac)}/parameters`, {
      method: 'POST',
      body: JSON.stringify({ [path]: value }),
    });
  },

  // Device Actions
  async reboot(mac: string): Promise<Tr069Job> {
    return request<Tr069Job>(`/devices/${encodeURIComponent(mac)}/reboot`, {
      method: 'POST',
    });
  },

  async factoryReset(mac: string): Promise<Tr069Job> {
    return request<Tr069Job>(`/devices/${encodeURIComponent(mac)}/factory-reset`, {
      method: 'POST',
    });
  },

  async triggerConnectionRequest(mac: string): Promise<{ isSuccessful: boolean; message: string }> {
    return request<{ isSuccessful: boolean; message: string }>(`/devices/${encodeURIComponent(mac)}/connection-request`, {
      method: 'POST',
    });
  },

  // Telemetry & Historical stats
  async getTelemetry(mac: string): Promise<{ latest: Tr069Telemetry; history: Tr069Telemetry[] }> {
    return request<{ latest: Tr069Telemetry; history: Tr069Telemetry[] }>(`/devices/${encodeURIComponent(mac)}/telemetry`);
  },

  // Faults
  async getFaults(mac: string, limit = 20): Promise<Tr069FaultRecord[]> {
    return request<Tr069FaultRecord[]>(`/devices/${encodeURIComponent(mac)}/faults?limit=${limit}`);
  },

  // Jobs
  async getJobs(mac: string): Promise<Tr069Job[]> {
    return request<Tr069Job[]>(`/devices/${encodeURIComponent(mac)}/jobs`);
  },
};
