package com.openomada.plugin.tr069.connectionrequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpConnectTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Enterprise service responsible for issuing TR-069 Connection Requests to CPEs
 * via HTTP GET with HTTP Basic and Digest Authentication, rate limiting, and timeout protections.
 */
@Service
public class ConnectionRequestService {
    private static final Logger log = LoggerFactory.getLogger(ConnectionRequestService.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final long MIN_REQUEST_INTERVAL_MS = 3000; // 3 seconds rate limit per target

    private final Map<String, Long> lastRequestTimeByTarget = new ConcurrentHashMap<>();

    /**
     * Synchronously triggers a Connection Request on the CPE.
     */
    public ConnectionRequestResult sendConnectionRequest(String connectionRequestUrl, String username, String password) {
        try {
            return sendConnectionRequestAsync(connectionRequestUrl, username, password)
                    .get(REQUEST_TIMEOUT.toSeconds() + 2, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            return ConnectionRequestResult.failure(ConnectionRequestResult.Status.TIMEOUT, 0, "Connection Request timed out after " + REQUEST_TIMEOUT.toSeconds() + "s", REQUEST_TIMEOUT.toMillis());
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return ConnectionRequestResult.failure(ConnectionRequestResult.Status.FAILED, 0, cause.getMessage(), 0);
        }
    }

    /**
     * Asynchronously triggers a Connection Request on the CPE.
     */
    public CompletableFuture<ConnectionRequestResult> sendConnectionRequestAsync(String connectionRequestUrl, String username, String password) {
        if (connectionRequestUrl == null || connectionRequestUrl.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ConnectionRequestResult.failure(ConnectionRequestResult.Status.INVALID_URL, 0, "Empty ConnectionRequestURL", 0));
        }

        URI uri;
        try {
            uri = URI.create(connectionRequestUrl.trim());
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(
                    ConnectionRequestResult.failure(ConnectionRequestResult.Status.INVALID_URL, 0, "Malformed URL: " + e.getMessage(), 0));
        }

        // Rate limiting per host/port
        String targetKey = uri.getHost() + ":" + (uri.getPort() > 0 ? uri.getPort() : 80);
        long now = System.currentTimeMillis();
        Long lastTime = lastRequestTimeByTarget.get(targetKey);
        if (lastTime != null && (now - lastTime) < MIN_REQUEST_INTERVAL_MS) {
            log.warn("Rate limiting connection request to {}", targetKey);
            return CompletableFuture.completedFuture(
                    ConnectionRequestResult.failure(ConnectionRequestResult.Status.RATE_LIMITED, 429,
                            "Rate limited: wait " + (MIN_REQUEST_INTERVAL_MS - (now - lastTime)) + "ms", 0));
        }
        lastRequestTimeByTarget.put(targetKey, now);

        long start = System.currentTimeMillis();

        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (username != null && !username.isEmpty()) {
            clientBuilder.authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password != null ? password.toCharArray() : new char[0]);
                }
            });
        }

        HttpClient client = clientBuilder.build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "OpenOmada-ACS/1.0")
                .header("Accept", "*/*")
                .GET()
                .build();

        log.info("Sending TR-069 Connection Request to {}", connectionRequestUrl);

        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(response -> {
                    long latency = System.currentTimeMillis() - start;
                    int statusCode = response.statusCode();

                    if (statusCode == 200 || statusCode == 204) {
                        log.info("CPE accepted connection request from {} in {}ms (HTTP {})", connectionRequestUrl, latency, statusCode);
                        return ConnectionRequestResult.success(statusCode, latency);
                    } else if (statusCode == 401) {
                        log.warn("CPE rejected connection request (Unauthorized 401) at {}", connectionRequestUrl);
                        return ConnectionRequestResult.failure(ConnectionRequestResult.Status.UNAUTHORIZED, statusCode, "Authentication failed (HTTP 401)", latency);
                    } else {
                        log.warn("CPE returned HTTP {} for connection request at {}", statusCode, connectionRequestUrl);
                        return ConnectionRequestResult.failure(ConnectionRequestResult.Status.FAILED, statusCode, "HTTP Status " + statusCode, latency);
                    }
                })
                .exceptionally(ex -> {
                    long latency = System.currentTimeMillis() - start;
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    log.error("Failed to deliver connection request to {}: {}", connectionRequestUrl, cause.getMessage());

                    if (cause instanceof HttpConnectTimeoutException || cause instanceof TimeoutException) {
                        return ConnectionRequestResult.failure(ConnectionRequestResult.Status.TIMEOUT, 0, "Connect/Read Timeout: " + cause.getMessage(), latency);
                    }
                    return ConnectionRequestResult.failure(ConnectionRequestResult.Status.FAILED, 0, cause.getMessage(), latency);
                });
    }
}
