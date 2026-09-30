import React from 'react';

export type StatusType = 'connected' | 'online' | 'good' | 'disconnected' | 'offline' | 'poor' | 'fair' | 'pending' | 'nodata';

interface OmadaStatusDotProps {
  status: StatusType | string;
  label?: string;
  showText?: boolean;
}

const STATUS_CONFIG: Record<string, { color: string; defaultLabel: string }> = {
  connected: { color: '#05C178', defaultLabel: 'Connected' },
  online: { color: '#05C178', defaultLabel: 'Online' },
  good: { color: '#05C178', defaultLabel: 'Good' },
  fair: { color: '#FAAD14', defaultLabel: 'Fair' },
  pending: { color: '#FAAD14', defaultLabel: 'Pending' },
  disconnected: { color: '#EE385C', defaultLabel: 'Disconnected' },
  offline: { color: '#EE385C', defaultLabel: 'Offline' },
  poor: { color: '#EE385C', defaultLabel: 'Poor' },
  nodata: { color: '#CCCCCC', defaultLabel: 'No Data' },
};

export const OmadaStatusDot: React.FC<OmadaStatusDotProps> = ({ status, label, showText = true }) => {
  const normalizedKey = status.toLowerCase();
  const config = STATUS_CONFIG[normalizedKey] || { color: '#05C178', defaultLabel: status };
  const displayText = label || config.defaultLabel;

  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', color: '#2B2B2B' }}>
      <span
        style={{
          width: '7px',
          height: '7px',
          borderRadius: '50%',
          backgroundColor: config.color,
          display: 'inline-block',
          flexShrink: 0,
        }}
      />
      {showText && <span>{displayText}</span>}
    </span>
  );
};
