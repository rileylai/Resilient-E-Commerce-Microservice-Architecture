package com.tut2.group3.store.util;

import org.springframework.util.DigestUtils;

public class Md5Utils {

    public static String md5(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }

}
