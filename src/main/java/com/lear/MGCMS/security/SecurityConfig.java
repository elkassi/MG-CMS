package com.lear.MGCMS.security;

import static com.lear.MGCMS.security.SecurityConstants.H2_URL;
import static com.lear.MGCMS.security.SecurityConstants.SIGN_UP_URLS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.lear.MGCMS.services.CustomUserDetailsService;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true,
        prePostEnabled = true
)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private CmsProdIntegrationAuthenticationFilter cmsProdIntegrationAuthenticationFilter;
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {return new JwtAuthenticationFilter();}
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // .and()
                // .headers().frameOptions().sameOrigin() // to enaable H2 Database
                .and()
                .authorizeRequests()
                .antMatchers(
                        "/",
                        "/favicon.ico",
                        "/**/*.png",
                        "/**/*.jpg",
                        "/*.png",
                        "/*.jpg",
                        "/**/*.gif",
                        "/**/*.svg",
                        "/**/*.json",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js",
                        "/login", "/{file:(?!bundle\\.js|api)[a-zA-Z0-9_-]*}",  "/{file:(?!bundle\\.js|api)[a-zA-Z0-9_-]*}/**"
                ).permitAll()
                .antMatchers(HttpMethod.POST, "/api/storageHistory/uploadFile").permitAll()
                .antMatchers(SIGN_UP_URLS).permitAll()
                // password-reset flow happens logged-out; resetFast is gated by a
                // one-shot random code with a 6h expiry, emailConfirmation only
                // emails the account's registered address
                .antMatchers(HttpMethod.GET, "/api/user/emailConfirmation").permitAll()
                .antMatchers(HttpMethod.GET, "/api/user/resetFast/**").permitAll()
                .antMatchers(H2_URL).permitAll()
                // Phase 8 shopfloor kiosks have no login — only the kiosk
                // namespace is exempt; every other API GET requires auth.
                .antMatchers(HttpMethod.GET, "/api/kiosk/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                .anyRequest().authenticated();

        http.addFilterBefore(cmsProdIntegrationAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(customUserDetailsService).passwordEncoder(bCryptPasswordEncoder);
	}

	@Override
	@Bean(BeanIds.AUTHENTICATION_MANAGER)
	protected AuthenticationManager authenticationManager() throws Exception {
		return super.authenticationManager();
	}
    
	
    
}
