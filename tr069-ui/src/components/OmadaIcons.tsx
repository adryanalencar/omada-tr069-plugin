import React from 'react';

export const SearchIcon: React.FC<{ size?: number; color?: string }> = ({ size = 14, color = '#999999' }) => (
  <svg width={size} height={size} viewBox="0 0 16 16" fill="none">
    <path
      d="M7.333 12.667A5.333 5.333 0 1 0 7.333 2a5.333 5.333 0 0 0 0 10.667ZM14 14l-2.9-2.9"
      stroke={color}
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

export const PlusIcon: React.FC<{ size?: number; color?: string }> = ({ size = 14, color = 'currentColor' }) => (
  <svg width={size} height={size} viewBox="0 0 16 16" fill="none">
    <path d="M8 3.333v9.334M3.333 8h9.334" stroke={color} strokeWidth="1.6" strokeLinecap="round" />
  </svg>
);

export const ExportIcon: React.FC<{ size?: number; color?: string }> = ({ size = 14, color = '#00A870' }) => (
  <svg width={size} height={size} viewBox="0 0 16 16" fill="none">
    <path
      d="M14 10v2.667A1.333 1.333 0 0 1 12.667 14H3.333A1.333 1.333 0 0 1 2 12.667V10M11.333 6.667 8 10 4.667 6.667M8 10V2"
      stroke={color}
      strokeWidth="1.3"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

export const LocateIcon: React.FC<{ size?: number; color?: string; onClick?: () => void; title?: string; className?: string }> = ({
  size = 16,
  color = '#00A870',
  onClick,
  title = 'Localizar',
  className = '',
}) => (
  <span
    onClick={onClick}
    title={title}
    className={className}
    style={{ cursor: onClick ? 'pointer' : 'default', display: 'inline-flex', alignItems: 'center' }}
  >
    <svg width={size} height={size} viewBox="0 0 16 16" fill="none">
      <circle cx="8" cy="8" r="6" stroke={color} strokeWidth="1.3" />
      <circle cx="8" cy="8" r="2.5" stroke={color} strokeWidth="1.3" />
      <path d="M8 1v2M8 13v2M1 8h2M13 8h2" stroke={color} strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  </span>
);

export const PowerIcon: React.FC<{ size?: number; color?: string; onClick?: () => void; title?: string; className?: string }> = ({
  size = 16,
  color = '#00A870',
  onClick,
  title = 'Reboot / Reiniciar',
  className = '',
}) => (
  <span
    onClick={onClick}
    title={title}
    className={className}
    style={{ cursor: onClick ? 'pointer' : 'default', display: 'inline-flex', alignItems: 'center' }}
  >
    <svg width={size} height={size} viewBox="0 0 16 16" fill="none">
      <path d="M8 2v5" stroke={color} strokeWidth="1.4" strokeLinecap="round" />
      <path
        d="M4.3 4.3a5.5 5.5 0 1 0 7.4 0"
        stroke={color}
        strokeWidth="1.4"
        strokeLinecap="round"
      />
    </svg>
  </span>
);

export const EditIcon: React.FC<{ size?: number; color?: string; onClick?: () => void; title?: string; className?: string }> = ({
  size = 16,
  color = '#00A870',
  onClick,
  title = 'Editar',
}) => (
  <span
    onClick={onClick}
    title={title}
    style={{ cursor: onClick ? 'pointer' : 'default', display: 'inline-flex', alignItems: 'center' }}
  >
    <svg width={size} height={size} viewBox="0 0 16 16" fill="none">
      <path
        d="M11.333 2.667a1.886 1.886 0 1 1 2.667 2.666L5.333 14H2.667v-2.667l8.666-8.666Z"
        stroke={color}
        strokeWidth="1.3"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  </span>
);

export const TrashIcon: React.FC<{ size?: number; color?: string; onClick?: () => void; title?: string }> = ({
  size = 16,
  color = '#00A870',
  onClick,
  title = 'Excluir',
}) => (
  <span
    onClick={onClick}
    title={title}
    style={{ cursor: onClick ? 'pointer' : 'default', display: 'inline-flex', alignItems: 'center' }}
  >
    <svg width={size} height={size} viewBox="0 0 16 16" fill="none">
      <path
        d="M2.667 4h10.666M5.333 4V2.667a1.333 1.333 0 0 1 1.334-1.334h2.666a1.333 1.333 0 0 1 1.334 1.334V4m2 0v9.333a1.333 1.333 0 0 1-1.334 1.334H4.667a1.333 1.333 0 0 1-1.334-1.334V4"
        stroke={color}
        strokeWidth="1.3"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  </span>
);

export const HelpCircleIcon: React.FC<{ size?: number; color?: string }> = ({ size = 18, color = '#00A870' }) => (
  <svg width={size} height={size} viewBox="0 0 18 18" fill="none">
    <circle cx="9" cy="9" r="7.5" stroke={color} strokeWidth="1.3" />
    <path
      d="M7.2 6.8a2 2 0 0 1 3.6 1.2c0 1.2-1.8 1.6-1.8 2.8M9 13.5h.01"
      stroke={color}
      strokeWidth="1.4"
      strokeLinecap="round"
    />
  </svg>
);

export const DeviceApIcon: React.FC<{ size?: number }> = ({ size = 22 }) => (
  <div
    style={{
      width: size,
      height: size,
      borderRadius: '4px',
      backgroundColor: '#EBEBEB',
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      flexShrink: 0,
    }}
  >
    <svg width={14} height={14} viewBox="0 0 16 16" fill="none">
      <rect x="3" y="3" width="10" height="10" rx="2" fill="#FFFFFF" stroke="#999999" strokeWidth="1.2" />
      <circle cx="8" cy="8" r="1.5" fill="#00A870" />
    </svg>
  </div>
);

export const DeviceGatewayIcon: React.FC<{ size?: number }> = ({ size = 22 }) => (
  <div
    style={{
      width: size,
      height: size,
      borderRadius: '4px',
      backgroundColor: '#EBEBEB',
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      flexShrink: 0,
    }}
  >
    <svg width={14} height={14} viewBox="0 0 16 16" fill="none">
      <rect x="2" y="5" width="12" height="6" rx="1.5" fill="#FFFFFF" stroke="#999999" strokeWidth="1.2" />
      <line x1="4" y1="8" x2="6" y2="8" stroke="#00A870" strokeWidth="1.2" strokeLinecap="round" />
      <line x1="8" y1="8" x2="10" y2="8" stroke="#999999" strokeWidth="1.2" strokeLinecap="round" />
    </svg>
  </div>
);
