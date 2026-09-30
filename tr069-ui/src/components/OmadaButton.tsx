import React from 'react';

export type ButtonVariant = 'primary' | 'default' | 'link' | 'danger';

interface OmadaButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  icon?: React.ReactNode;
  children?: React.ReactNode;
  size?: 'small' | 'medium';
}

export const OmadaButton: React.FC<OmadaButtonProps> = ({
  variant = 'primary',
  icon,
  children,
  size = 'medium',
  style,
  disabled,
  ...props
}) => {
  const isSmall = size === 'small';
  const height = isSmall ? '26px' : '32px';
  const padding = isSmall ? '0 10px' : '0 16px';
  const fontSize = isSmall ? '12px' : '13px';

  let baseStyle: React.CSSProperties = {
    height,
    padding,
    fontSize,
    borderRadius: '6px',
    fontWeight: 600,
    cursor: disabled ? 'not-allowed' : 'pointer',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '6px',
    userSelect: 'none',
    transition: 'all 0.15s ease',
    fontFamily: 'inherit',
    boxSizing: 'border-box',
    border: 'none',
    outline: 'none',
    opacity: disabled ? 0.5 : 1,
    ...style,
  };

  if (variant === 'primary') {
    baseStyle = {
      ...baseStyle,
      backgroundColor: '#00A870',
      color: '#FFFFFF',
      boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)',
    };
  } else if (variant === 'default') {
    baseStyle = {
      ...baseStyle,
      backgroundColor: '#FFFFFF',
      color: '#2B2B2B',
      border: '1px solid #D9D9D9',
      fontWeight: 500,
    };
  } else if (variant === 'link') {
    baseStyle = {
      ...baseStyle,
      backgroundColor: 'transparent',
      color: '#00A870',
      padding: '0 4px',
      fontWeight: 500,
    };
  } else if (variant === 'danger') {
    baseStyle = {
      ...baseStyle,
      backgroundColor: '#EE385C',
      color: '#FFFFFF',
    };
  }

  return (
    <button
      {...props}
      disabled={disabled}
      style={baseStyle}
      onMouseEnter={(e) => {
        if (disabled) return;
        if (variant === 'primary') e.currentTarget.style.backgroundColor = '#05C178';
        if (variant === 'default') {
          e.currentTarget.style.borderColor = '#00A870';
          e.currentTarget.style.color = '#00A870';
        }
      }}
      onMouseLeave={(e) => {
        if (disabled) return;
        if (variant === 'primary') e.currentTarget.style.backgroundColor = '#00A870';
        if (variant === 'default') {
          e.currentTarget.style.borderColor = '#D9D9D9';
          e.currentTarget.style.color = '#2B2B2B';
        }
      }}
    >
      {icon}
      {children}
    </button>
  );
};
