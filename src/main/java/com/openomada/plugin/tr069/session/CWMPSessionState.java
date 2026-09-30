package com.openomada.plugin.tr069.session;

public enum CWMPSessionState {
    INITIAL,
    INFORM_RECEIVED,
    INFORM_RESPONDED,
    ACS_REQUESTING,
    CPE_RESPONDING,
    TERMINATING,
    TERMINATED,
    TIMED_OUT,
    ERROR
}
