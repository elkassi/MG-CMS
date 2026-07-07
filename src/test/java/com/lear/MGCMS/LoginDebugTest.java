package com.lear.MGCMS;

import com.lear.MGCMS.domain.User;
import com.lear.MGCMS.repositories.UserRepository;
import com.lear.MGCMS.services.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;

@SpringBootTest
public class LoginDebugTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Test
    public void debugLogin() {
        User user = userRepository.findByUsername("melghazi");
        System.out.println("User found: " + (user != null));
        if (user != null) {
            System.out.println("Active: " + user.isActive());
            System.out.println("Roles: " + user.getRoles());
            try {
                UserDetails ud = userDetailsService.loadUserByUsername("melghazi");
                System.out.println("UserDetails: " + ud);
                System.out.println("Authorities: " + ud.getAuthorities());
            } catch (Exception e) {
                System.out.println("Exception: " + e.getClass().getName() + " - " + e.getMessage());
            }
        }
    }
}
