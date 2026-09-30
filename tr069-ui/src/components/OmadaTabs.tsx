import React from 'react';
import { HelpCircleIcon } from './OmadaIcons';

export interface TabItem {
  id: string;
  label: string;
  badge?: string | number;
}

interface OmadaTabsProps {
  tabs: TabItem[];
  activeTab: string;
  onChange: (tabId: string) => void;
  showHelp?: boolean;
  onHelpClick?: () => void;
  rightActions?: React.ReactNode;
}

export const OmadaTabs: React.FC<OmadaTabsProps> = ({
  tabs,
  activeTab,
  onChange,
  showHelp = true,
  onHelpClick,
  rightActions,
}) => {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderBottom: '1px solid #ECECEC',
        marginBottom: '16px',
        paddingBottom: '0',
      }}
    >
      <div style={{ display: 'flex', gap: '32px' }}>
        {tabs.map((tab) => {
          const isActive = tab.id === activeTab;
          return (
            <div
              key={tab.id}
              onClick={() => onChange(tab.id)}
              style={{
                paddingBottom: '12px',
                fontSize: '14px',
                fontWeight: isActive ? 600 : 500,
                color: isActive ? '#00A870' : '#636363',
                cursor: 'pointer',
                position: 'relative',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                transition: 'color 0.15s ease',
                userSelect: 'none',
              }}
            >
              <span>{tab.label}</span>
              {tab.badge !== undefined && (
                <span
                  style={{
                    backgroundColor: isActive ? 'rgba(0, 168, 112, 0.12)' : '#F0F0F0',
                    color: isActive ? '#00A870' : '#636363',
                    fontSize: '11px',
                    fontWeight: 600,
                    padding: '1px 6px',
                    borderRadius: '10px',
                  }}
                >
                  {tab.badge}
                </span>
              )}
              {isActive && (
                <div
                  style={{
                    position: 'absolute',
                    bottom: '-1px',
                    left: 0,
                    right: 0,
                    height: '2px',
                    backgroundColor: '#00A870',
                    borderRadius: '2px 2px 0 0',
                  }}
                />
              )}
            </div>
          );
        })}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {rightActions}
        {showHelp && (
          <div
            onClick={onHelpClick}
            style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center' }}
            title="Help / Documentation"
          >
            <HelpCircleIcon size={18} color="#00A870" />
          </div>
        )}
      </div>
    </div>
  );
};
