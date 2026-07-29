package com.ordering.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityConfig {

    private List<UserDef> users;

    @Data
    public static class UserDef {
        private String username;
        private String password;
        private String roles;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/categories", "/api/dishes", "/api/dishes/*", "/api/combos").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tables", "/api/tables/number/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/orders/stats").hasAnyRole("ADMIN", "KITCHEN")
                .requestMatchers(HttpMethod.GET, "/api/orders", "/api/orders/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders", "/api/waiter-calls").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/doc.html", "/swagger-resources/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/categories", "/api/dishes", "/api/combos", "/api/tables").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/categories/*", "/api/dishes/*", "/api/combos/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/*", "/api/dishes/*", "/api/combos/*", "/api/tables/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/tables/*", "/api/orders/**").hasAnyRole("ADMIN", "KITCHEN")
                .requestMatchers(HttpMethod.PUT, "/api/waiter-calls/*").hasAnyRole("ADMIN", "KITCHEN")
                .requestMatchers(HttpMethod.GET, "/api/waiter-calls").hasAnyRole("ADMIN", "KITCHEN")
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.authenticationEntryPoint(basicAuthEntryPoint()));

        return http.build();
    }

    @Bean
    public BasicAuthenticationEntryPoint basicAuthEntryPoint() {
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("OrderingSystem");
        return entryPoint;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        if (users != null) {
            for (UserDef u : users) {
                manager.createUser(User.withUsername(u.getUsername())
                        .password("{noop}" + u.getPassword())
                        .roles(u.getRoles().split(","))
                        .build());
            }
        }
        return manager;
    }
}
