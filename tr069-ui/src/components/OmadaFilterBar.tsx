import React from 'react';
import { SearchIcon } from './OmadaIcons';

export interface FilterPill {
  id: string;
  label: string;
  count?: number;
  dotColor?: string;
}

interface OmadaFilterBarProps {
  searchValue?: string;
  onSearchChange?: (val: string) => void;
  searchPlaceholder?: string;
  pills?: FilterPill[];
  activePill?: string;
  onPillChange?: (id: string) => void;
  statusPills?: FilterPill[];
  activeStatusPill?: string;
  onStatusPillChange?: (id: string) => void;
  rightActions?: React.ReactNode;
}

export const OmadaFilterBar: React.FC<OmadaFilterBarProps> = ({
  searchValue = '',
  onSearchChange,
  searchPlaceholder = 'Name, SN, MAC, IPv4, Model or L...',
  pills = [],
  activePill,
  onPillChange,
  statusPills = [],
  activeStatusPill,
  onStatusPillChange,
  rightActions,
}) => {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '12px',
        marginBottom: '16px',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
        {/* Search Input */}
        {onSearchChange && (
          <div style={{ position: 'relative', width: '280px' }}>
            <span
              style={{
                position: 'absolute',
                left: '10px',
                top: '50%',
                transform: 'translateY(-50%)',
                pointerEvents: 'none',
                display: 'flex',
                alignItems: 'center',
              }}
            >
              <SearchIcon size={14} color="#999999" />
            </span>
            <input
              type="text"
              value={searchValue}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder={searchPlaceholder}
              style={{
                width: '100%',
                height: '32px',
                padding: '4px 10px 4px 32px',
                borderRadius: '6px',
                border: '1px solid #D9D9D9',
                backgroundColor: '#FFFFFF',
                color: '#2B2B2B',
                fontSize: '13px',
                outline: 'none',
                boxSizing: 'border-box',
                transition: 'border-color 0.15s ease',
                fontFamily: 'inherit',
              }}
              onFocus={(e) => (e.target.style.borderColor = '#00A870')}
              onBlur={(e) => (e.target.style.borderColor = '#D9D9D9')}
            />
          </div>
        )}

        {/* Category Pills (e.g. All, GPON, Wi-Fi) */}
        {pills.length > 0 && (
          <div
            style={{
              display: 'inline-flex',
              backgroundColor: '#F5F5F5',
              padding: '2px',
              borderRadius: '6px',
              border: '1px solid #ECECEC',
            }}
          >
            {pills.map((pill) => {
              const isActive = pill.id === activePill;
              return (
                <button
                  key={pill.id}
                  onClick={() => onPillChange && onPillChange(pill.id)}
                  style={{
                    padding: '4px 12px',
                    borderRadius: '4px',
                    border: 'none',
                    backgroundColor: isActive ? '#FFFFFF' : 'transparent',
                    color: isActive ? '#00A870' : '#636363',
                    fontSize: '12px',
                    fontWeight: isActive ? 600 : 500,
                    cursor: 'pointer',
                    boxShadow: isActive ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
                    transition: 'all 0.15s ease',
                    fontFamily: 'inherit',
                  }}
                >
                  {pill.label} {pill.count !== undefined && `(${pill.count})`}
                </button>
              );
            })}
          </div>
        )}

        {/* Status Pills with Dots (e.g. All, Good, Fair, Poor, No Data) */}
        {statusPills.length > 0 && (
          <div style={{ display: 'inline-flex', gap: '8px', alignItems: 'center' }}>
            {statusPills.map((sp) => {
              const isActive = sp.id === activeStatusPill;
              return (
                <button
                  key={sp.id}
                  onClick={() => onStatusPillChange && onStatusPillChange(sp.id)}
                  style={{
                    padding: '4px 10px',
                    borderRadius: '6px',
                    border: isActive ? '1px solid #00A870' : '1px solid #EBEBEB',
                    backgroundColor: isActive ? 'rgba(0, 168, 112, 0.08)' : '#FFFFFF',
                    color: isActive ? '#00A870' : '#636363',
                    fontSize: '12px',
                    fontWeight: isActive ? 600 : 500,
                    cursor: 'pointer',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '5px',
                    transition: 'all 0.15s ease',
                    fontFamily: 'inherit',
                  }}
                >
                  {sp.dotColor && (
                    <span
                      style={{
                        width: '6px',
                        height: '6px',
                        borderRadius: '50%',
                        backgroundColor: sp.dotColor,
                        display: 'inline-block',
                      }}
                    />
                  )}
                  {sp.label} {sp.count !== undefined && `(${sp.count})`}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Right Action Buttons */}
      {rightActions && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {rightActions}
        </div>
      )}
    </div>
  );
};
