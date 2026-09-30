import React from 'react';
import { Server, Shield, Globe, HelpCircle, CheckCircle, Copy, AlertCircle } from 'lucide-react';
import { OmadaButton } from './OmadaButton';

export const TR069ServerConfig: React.FC = () => {
  const host = window.location.hostname || '2.24.202.99';
  const acsUrl = `http://${host}:8088/tr069`;

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    alert('Copied to clipboard: ' + text);
  };

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
      {/* 1. ACS Server Details Card */}
      <div style={{ background: '#FFFFFF', border: '1px solid var(--omada-border, #ECECEC)', borderRadius: '8px', padding: '24px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
          <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: 'rgba(0, 168, 112, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#00A870' }}>
            <Server size={20} />
          </div>
          <div>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600, color: '#2B2B2B' }}>
              Embedded Omada ACS Server (CWMP 1.4)
            </h3>
            <div style={{ fontSize: '12px', color: '#636363' }}>
              Native service running inside Omada Controller process (port 8088)
            </div>
          </div>
        </div>

        <table className="omada-table" style={{ marginBottom: '16px' }}>
          <tbody>
            <tr>
              <td style={{ width: '35%', color: '#636363', fontWeight: 500 }}>ACS Server URL (CWMP URL)</td>
              <td>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <code style={{ fontFamily: 'var(--font-mono)', fontSize: '13px', color: '#00A870', fontWeight: 600, background: '#F7F7F7', padding: '4px 8px', borderRadius: '4px' }}>
                    {acsUrl}
                  </code>
                  <OmadaButton variant="default" style={{ height: '26px', padding: '0 8px', fontSize: '11px' }} onClick={() => copyToClipboard(acsUrl)}>
                    <Copy size={12} /> Copy
                  </OmadaButton>
                </div>
              </td>
            </tr>
            <tr>
              <td style={{ color: '#636363', fontWeight: 500 }}>Alternate Alias / Endpoint</td>
              <td>
                <code style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', color: '#2B2B2B' }}>
                  http://{host}:8088/service/tr069
                </code>
              </td>
            </tr>
            <tr>
              <td style={{ color: '#636363', fontWeight: 500 }}>HTTP Authentication (CPE ➔ ACS)</td>
              <td>
                <span className="omada-badge omada-badge-info">
                  Optional / Digest or Basic (Default: omada / omada or blank)
                </span>
              </td>
            </tr>
            <tr>
              <td style={{ color: '#636363', fontWeight: 500 }}>Recommended Interval (Periodic Inform)</td>
              <td>
                <strong>10 seconds</strong> (for lab/testing telemetry) or <strong>60 seconds</strong> (production)
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      {/* 2. Vendor Setup Guides */}
      <div style={{ background: '#FFFFFF', border: '1px solid var(--omada-border, #ECECEC)', borderRadius: '8px', padding: '24px' }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '15px', fontWeight: 600, color: '#2B2B2B', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <HelpCircle size={18} color="#00A870" />
          Client Routers & ONTs Configuration Guide
        </h3>

        {/* ZTE Section */}
        <div style={{ background: '#FAFAFA', border: '1px solid #ECECEC', borderRadius: '6px', padding: '16px', marginBottom: '14px' }}>
          <div style={{ fontWeight: 600, color: '#2B2B2B', fontSize: '14px', marginBottom: '6px' }}>
            1. ZTE ONTs (ZXHN F6600P, F680, F670L)
          </div>
          <div style={{ fontSize: '13px', color: '#636363', lineHeight: 1.6 }}>
            <ul style={{ paddingLeft: '20px', marginTop: '6px' }}>
              <li>
                <strong>WAN Connection Creation</strong>: Navigate to <em>Internet ➔ WAN</em>. If the add WAN option is disabled, ensure the device operation mode is set to <strong>Router (PPPoE/DHCP)</strong> or log in using administrative credentials (e.g. <code>admin</code> or ISP credentials).
              </li>
              <li>
                <strong>Service List</strong>: The WAN connection must include the <code>TR069</code> flag (or <code>TR069_INTERNET</code>). Without this flag, ZTE internal firewall drops outgoing HTTP packets to the ACS.
              </li>
              <li>
                <strong>ACS Server</strong>: Navigate to <em>Management & Diagnosis ➔ TR-069</em>:
                <div style={{ background: '#FFFFFF', padding: '8px 12px', borderRadius: '4px', border: '1px solid #E0E0E0', marginTop: '6px', fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
                  URL: {acsUrl}<br/>
                  Periodic Inform: Enable<br/>
                  Periodic Inform Interval: 10
                </div>
              </li>
            </ul>
          </div>
        </div>

        {/* Huawei Section */}
        <div style={{ background: '#FAFAFA', border: '1px solid #ECECEC', borderRadius: '6px', padding: '16px', marginBottom: '14px' }}>
          <div style={{ fontWeight: 600, color: '#2B2B2B', fontSize: '14px', marginBottom: '6px' }}>
            2. Huawei ONTs (EchoLife HG8145V5, EG8145X6, HG8245H)
          </div>
          <div style={{ fontSize: '13px', color: '#636363', lineHeight: 1.6 }}>
            <ul style={{ paddingLeft: '20px', marginTop: '6px' }}>
              <li>
                Navigate to <em>WAN ➔ WAN Configuration</em> and select <code>Service Type: TR069_INTERNET</code> on the active WAN.
              </li>
              <li>
                Navigate to <em>System Tools ➔ TR-069</em>, enable the service, and set the URL to <code>{acsUrl}</code>.
              </li>
            </ul>
          </div>
        </div>

        {/* TP-Link Section */}
        <div style={{ background: '#FAFAFA', border: '1px solid #ECECEC', borderRadius: '6px', padding: '16px' }}>
          <div style={{ fontWeight: 600, color: '#2B2B2B', fontSize: '14px', marginBottom: '6px' }}>
            3. TP-Link Routers (Archer C6, EX220, EC220)
          </div>
          <div style={{ fontSize: '13px', color: '#636363', lineHeight: 1.6 }}>
            <ul style={{ paddingLeft: '20px', marginTop: '6px' }}>
              <li>
                Navigate to <em>System Tools ➔ CWMP Settings</em>.
              </li>
              <li>
                Enable CWMP Client and paste the ACS URL <code>{acsUrl}</code>. Set the Inform Interval to 60s.
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};
