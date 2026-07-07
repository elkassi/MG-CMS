package com.lear.MGCMS;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TempHashGenTest {
    @Test
    public void printHash() {
        System.out.println("HASH=" + new BCryptPasswordEncoder().encode("azerty@123456"));
    }
}
