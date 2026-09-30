package com.openomada.plugin.tr069.connectionrequest;

public class ConnectionRequestResult {
    public enum Status {
        SUCCESS,
        FAILED,
        TIMEOUT,
        UNAUTHORIZED,
        RATE_LIMITED,
        INVALID_URL
    }

    private final Status status;
    private final int httpStatusCode;
    private final String message;
    private final long latencyMs;

    public ConnectionRequestResult(Status status, int httpStatusCode, String message, long latencyMs) {
        this.status = status;
        this.httpStatusCode = httpStatusCode;
        this.message = message;
        this.latencyMs = latencyMs;
    }

    public static ConnectionRequestResult success(int code, long latencyMs) {
        return new ConnectionRequestResult(Status.SUCCESS, code, "Connection request accepted by CPE", latencyMs);
    }

    public static ConnectionRequestResult failure(Status status, int code, String message, long latencyMs) {
        return new ConnectionRequestResult(status, code, message, latencyMs);
    }

    public Status getStatus() { return status; }
    public int getHttpStatusCode() { return httpStatusCode; }
    public String getMessage() { return message; }
    public long getLatencyMs() { return latencyMs; }
    public boolean isSuccessful() { return status == Status.SUCCESS; }

    @Override
    public String toString() {
        return "ConnectionRequestResult{" +
                "status=" + status +
                ", httpStatusCode=" + httpStatusCode +
                ", message='" + message + '\'' +
                ", latencyMs=" + latencyMs +
                '}';
    }
}
