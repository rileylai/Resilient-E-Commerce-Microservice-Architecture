package com.tut2.group3.deliveryco.util;

import com.auth0.jwt.interfaces.Claim;

import java.util.Map;

/**
 * ThreadLocal utility for storing JWT claims
 * Provides thread-safe storage for the current request's JWT claims
 */
public class ThreadLocalUtil {

    private static final ThreadLocal<Map<String, Claim>> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * Set JWT claims for the current thread
     *
     * @param claims Map of JWT claims
     */
    public static void set(Map<String, Claim> claims) {
        THREAD_LOCAL.set(claims);
    }

    /**
     * Get JWT claims for the current thread
     *
     * @return Map of JWT claims or null if not set
     */
    public static Map<String, Claim> get() {
        return THREAD_LOCAL.get();
    }

    /**
     * Remove JWT claims for the current thread
     * Should be called after request completion to prevent memory leaks
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
