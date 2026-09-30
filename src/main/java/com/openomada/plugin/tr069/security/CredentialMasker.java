package com.openomada.plugin.tr069.security;

/**
 * Utility for masking sensitive credentials in TR-069 logs, dumps, and API responses.
 */
public final class CredentialMasker {
    private CredentialMasker() {}

    public static String mask(String secret) {
        if (secret == null || secret.isEmpty()) return "";
        if (secret.length() <= 3) return "***";
        return secret.charAt(0) + "***" + secret.charAt(secret.length() - 1);
    }

    public static String maskXml(String xml) {
        if (xml == null) return null;
        return xml.replaceAll("(?i)(<Password[^>]*>)(.*?)(</Password>)", "$1******$3")
                  .replaceAll("(?i)(<KeyPassphrase[^>]*>)(.*?)(</KeyPassphrase>)", "$1******$3")
                  .replaceAll("(?i)(<PreSharedKey[^>]*>)(.*?)(</PreSharedKey>)", "$1******$3");
    }
}
