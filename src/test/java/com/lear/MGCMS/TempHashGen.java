package com.lear.MGCMS;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TempHashGen {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("azerty@123456"));
    }
}
