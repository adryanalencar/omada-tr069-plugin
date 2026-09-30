import React from 'react';

interface OmadaSwitchProps {
  checked: boolean;
  onChange?: (checked: boolean) => void;
  disabled?: boolean;
}

export const OmadaSwitch: React.FC<OmadaSwitchProps> = ({ checked, onChange, disabled }) => {
  return (
    <div
      onClick={() => {
        if (!disabled && onChange) {
          onChange(!checked);
        }
      }}
      style={{
        width: '38px',
        height: '20px',
        borderRadius: '10px',
        backgroundColor: checked ? '#00A870' : '#CCCCCC',
        position: 'relative',
        cursor: disabled ? 'not-allowed' : 'pointer',
        transition: 'background-color 0.2s ease',
        display: 'inline-block',
        verticalAlign: 'middle',
        userSelect: 'none',
        opacity: disabled ? 0.6 : 1,
      }}
    >
      <div
        style={{
          width: '16px',
          height: '16px',
          borderRadius: '50%',
          backgroundColor: '#FFFFFF',
          position: 'absolute',
          top: '2px',
          left: checked ? '20px' : '2px',
          transition: 'left 0.2s cubic-bezier(0.2, 0, 0, 1)',
          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.2)',
        }}
      />
    </div>
  );
};
