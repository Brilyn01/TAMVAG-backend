package com.tamvagbackend.security;

public final class ScopeAuthorization {

    private ScopeAuthorization() {
    }

    public static final String RISK_EVALUATE = "SCOPE_risk:evaluate";
    public static final String PROFILE_READ = "SCOPE_profile:read";
    public static final String CONSENT_CREATE = "SCOPE_consent:create";
    public static final String TRANSACTION_WRITE = "SCOPE_transaction:write";

    public static final String APPLICATION_READ = "SCOPE_application:read";
    public static final String APPLICATION_MANAGE = "SCOPE_application:manage";

    public static final String CONNECTOR_SYNC = "SCOPE_connector:sync";
}
