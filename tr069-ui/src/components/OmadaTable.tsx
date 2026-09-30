import React from 'react';

export interface Column<T> {
  key: string;
  title: string;
  render?: (row: T, index: number) => React.ReactNode;
  width?: string | number;
  align?: 'left' | 'center' | 'right';
  sortable?: boolean;
}

interface OmadaTableProps<T> {
  columns: Column<T>[];
  data: T[];
  rowKey: (row: T) => string | number;
  loading?: boolean;
  totalRecords?: number;
  pageSize?: number;
  currentPage?: number;
  onPageChange?: (page: number) => void;
  emptyText?: string;
  onRowClick?: (row: T) => void;
}

export function OmadaTable<T>({
  columns,
  data,
  rowKey,
  loading = false,
  totalRecords,
  pageSize = 10,
  currentPage = 1,
  onPageChange,
  emptyText = 'No Data',
  onRowClick,
}: OmadaTableProps<T>) {
  const total = totalRecords !== undefined ? totalRecords : data.length;
  const totalPages = Math.ceil(total / pageSize) || 1;

  const startRecord = total > 0 ? (currentPage - 1) * pageSize + 1 : 0;
  const endRecord = Math.min(currentPage * pageSize, total);

  return (
    <div
      style={{
        backgroundColor: '#FFFFFF',
        border: '1px solid #ECECEC',
        borderRadius: '8px',
        overflow: 'hidden',
        boxShadow: '0 1px 2px rgba(0, 0, 0, 0.02)',
      }}
    >
      <div style={{ overflowX: 'auto' }}>
        <table
          style={{
            width: '100%',
            borderCollapse: 'collapse',
            textAlign: 'left',
            fontSize: '13px',
            fontFamily: 'inherit',
          }}
        >
          <thead>
            <tr
              style={{
                backgroundColor: '#FAFAFA',
                borderBottom: '1px solid #ECECEC',
                height: '42px',
              }}
            >
              {columns.map((col) => (
                <th
                  key={col.key}
                  style={{
                    padding: '8px 16px',
                    fontWeight: 600,
                    color: '#2B2B2B',
                    fontSize: '13px',
                    width: col.width,
                    textAlign: col.align || 'left',
                    userSelect: 'none',
                    whiteSpace: 'nowrap',
                  }}
                >
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                    {col.title}
                    {col.sortable && (
                      <span style={{ color: '#999999', fontSize: '11px', cursor: 'pointer' }}>⇅</span>
                    )}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td
                  colSpan={columns.length}
                  style={{
                    padding: '36px',
                    textAlign: 'center',
                    color: '#00A870',
                    fontSize: '13px',
                  }}
                >
                  Carregando dados...
                </td>
              </tr>
            ) : data.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  style={{
                    padding: '36px',
                    textAlign: 'center',
                    color: '#999999',
                    fontSize: '13px',
                  }}
                >
                  {emptyText}
                </td>
              </tr>
            ) : (
              data.map((row, idx) => (
                <tr
                  key={rowKey(row)}
                  onClick={() => onRowClick && onRowClick(row)}
                  style={{
                    borderBottom: '1px solid #ECECEC',
                    transition: 'background-color 0.15s ease',
                    cursor: onRowClick ? 'pointer' : 'default',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#F7F7F7')}
                  onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                >
                  {columns.map((col) => {
                    const content = col.render ? col.render(row, idx) : (row as any)[col.key];
                    return (
                      <td
                        key={col.key}
                        style={{
                          padding: '12px 16px',
                          color: '#2B2B2B',
                          fontSize: '13px',
                          textAlign: col.align || 'left',
                          verticalAlign: 'middle',
                        }}
                      >
                        {content}
                      </td>
                    );
                  })}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer (Matching Screenshot 3) */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          alignItems: 'center',
          gap: '12px',
          padding: '10px 16px',
          borderTop: '1px solid #ECECEC',
          backgroundColor: '#FFFFFF',
          fontSize: '12px',
          color: '#636363',
          userSelect: 'none',
        }}
      >
        <span>
          Showing {startRecord}-{endRecord} of {total} records
        </span>

        {/* Previous page button */}
        <button
          disabled={currentPage <= 1}
          onClick={() => onPageChange && onPageChange(currentPage - 1)}
          style={{
            width: '26px',
            height: '26px',
            borderRadius: '4px',
            border: '1px solid #D9D9D9',
            backgroundColor: '#FFFFFF',
            color: currentPage <= 1 ? '#CCCCCC' : '#2B2B2B',
            cursor: currentPage <= 1 ? 'not-allowed' : 'pointer',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          ‹
        </button>

        {/* Page number button */}
        <button
          style={{
            minWidth: '26px',
            height: '26px',
            padding: '0 6px',
            borderRadius: '4px',
            border: '1px solid #00A870',
            backgroundColor: 'rgba(0, 168, 112, 0.1)',
            color: '#00A870',
            fontWeight: 600,
            cursor: 'pointer',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {currentPage}
        </button>

        {/* Next page button */}
        <button
          disabled={currentPage >= totalPages}
          onClick={() => onPageChange && onPageChange(currentPage + 1)}
          style={{
            width: '26px',
            height: '26px',
            borderRadius: '4px',
            border: '1px solid #D9D9D9',
            backgroundColor: '#FFFFFF',
            color: currentPage >= totalPages ? '#CCCCCC' : '#2B2B2B',
            cursor: currentPage >= totalPages ? 'not-allowed' : 'pointer',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          ›
        </button>

        {/* Page size dropdown */}
        <div
          style={{
            border: '1px solid #D9D9D9',
            borderRadius: '4px',
            padding: '2px 8px',
            backgroundColor: '#FFFFFF',
            fontSize: '12px',
            color: '#2B2B2B',
          }}
        >
          {pageSize} /page
        </div>
      </div>
    </div>
  );
}
