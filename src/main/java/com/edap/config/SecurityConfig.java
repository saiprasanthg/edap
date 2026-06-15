package com.edap.config;

import com.edap.security.JwtAuthenticationFilter;
import com.edap.security.JwtTokenProvider;
import com.edap.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                // Swagger / OpenAPI
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // RBAC rules
                // ADMIN: full access
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // ENGINEER+: create/update components
                .requestMatchers(HttpMethod.POST, "/api/components/**").hasAnyRole("ADMIN", "ENGINEER")
                .requestMatchers(HttpMethod.PUT, "/api/components/**").hasAnyRole("ADMIN", "ENGINEER")
                .requestMatchers(HttpMethod.DELETE, "/api/components/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/dependencies/**").hasAnyRole("ADMIN", "ENGINEER")
                .requestMatchers(HttpMethod.DELETE, "/api/dependencies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/teams/**").hasAnyRole("ADMIN", "ENGINEER")
                .requestMatchers(HttpMethod.PUT, "/api/teams/**").hasAnyRole("ADMIN", "ENGINEER")
                .requestMatchers(HttpMethod.DELETE, "/api/teams/**").hasRole("ADMIN")
                // All authenticated users can read
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
