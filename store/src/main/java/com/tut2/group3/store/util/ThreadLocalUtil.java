package com.tut2.group3.store.util;

import com.auth0.jwt.interfaces.Claim;


import java.util.Map;

public class ThreadLocalUtil {

    private static final ThreadLocal<Map<String, Claim>> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(Map<String, Claim> claims) {
        THREAD_LOCAL.set(claims);
    }

    /**
     * 获取当前线程的 claims
     */
    public static Map<String, Claim> get() {
        return THREAD_LOCAL.get();
    }

    /**
     * 清除当前线程的 claims（防止内存泄漏）
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
