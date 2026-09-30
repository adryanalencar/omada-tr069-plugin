import React from 'react';

export interface SummaryItem {
  id: string;
  title: string;
  icon?: React.ReactNode;
  subtitle?: string;
  metrics?: {
    total?: number | string;
    breakdown?: string;
    segments?: { color: string; widthPercent: number }[];
  };
}

interface OmadaSummaryBarProps {
  items: SummaryItem[];
}

export const OmadaSummaryBar: React.FC<OmadaSummaryBarProps> = ({ items }) => {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: `repeat(${items.length}, 1fr)`,
        gap: '16px',
        backgroundColor: '#FFFFFF',
        border: '1px solid #ECECEC',
        borderRadius: '8px',
        padding: '12px 24px',
        marginBottom: '16px',
        alignItems: 'center',
      }}
    >
      {items.map((item, idx) => (
        <div
          key={item.id}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            borderRight: idx < items.length - 1 ? '1px solid #F0F0F0' : 'none',
            paddingRight: '16px',
          }}
        >
          {item.icon && (
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '6px',
                backgroundColor: '#F5F5F5',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
                color: '#636363',
              }}
            >
              {item.icon}
            </div>
          )}

          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: '12px', color: '#636363', fontWeight: 500 }}>{item.title}</div>

            {item.subtitle && (
              <div style={{ fontSize: '13px', color: '#999999', marginTop: '2px', fontWeight: 500 }}>
                {item.subtitle}
              </div>
            )}

            {item.metrics && (
              <div style={{ marginTop: '2px' }}>
                <div style={{ fontSize: '14px', fontWeight: 700, color: '#2B2B2B', letterSpacing: '1px' }}>
                  {item.metrics.breakdown || item.metrics.total}
                </div>
                {item.metrics.segments && (
                  <div
                    style={{
                      display: 'flex',
                      height: '3px',
                      borderRadius: '2px',
                      overflow: 'hidden',
                      marginTop: '4px',
                      width: '100%',
                      backgroundColor: '#EBEBEB',
                    }}
                  >
                    {item.metrics.segments.map((seg, i) => (
                      <div
                        key={i}
                        style={{
                          backgroundColor: seg.color,
                          width: `${seg.widthPercent}%`,
                          height: '100%',
                        }}
                      />
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};
