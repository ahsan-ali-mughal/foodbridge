package com.foodbridge.admin.security;

/**
 * HttpSession attribute keys used to hold the admin's JWT after a
 * server-side login. admin-service is browser-facing and server-rendered,
 * so unlike the platform's other (API) services it does not accept bearer
 * tokens directly from the client — it authenticates the admin once via a
 * login form, exchanges credentials with auth-service itself, and keeps the
 * resulting JWT server-side in the session rather than exposing it to
 * client-side JavaScript.
 */
public final class SessionKeys {

    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String USER_ID = "USER_ID";
    public static final String DISPLAY_NAME = "DISPLAY_NAME";

    private SessionKeys() {
    }
}
