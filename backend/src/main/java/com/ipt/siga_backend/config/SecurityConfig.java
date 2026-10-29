package com.ipt.siga_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API with JWT: no browser session and no cookies, so CSRF protection is not needed
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Access rules. ORDER MATTERS: the first rule that matches wins.
                .authorizeHttpRequests(auth -> auth
                        // API documentation is public (no data in it)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Period registrations ("matrícula"): ADMIN only, even for GET
                        .requestMatchers("/api/periods/*/registrations").hasRole("ADMIN")
                                // Epic 6: the logged student manages HIS enrollments
                                .requestMatchers("/api/me/**").hasRole("STUDENT")
                            // Epic 6: ADMIN consults all the enrollments of a period (section 4.3)
                                .requestMatchers("/api/periods/*/enrollments").hasRole("ADMIN")
                        // Epic 5: any logged user reads periods and the academic offer; only ADMIN manages them
                        .requestMatchers(HttpMethod.GET, "/api/periods/**", "/api/sections/**").authenticated()
                        .requestMatchers("/api/periods/**", "/api/sections/**").hasRole("ADMIN")

                        // Epics 2-3: any logged user can READ careers, plans and subjects...
                        .requestMatchers(HttpMethod.GET, "/api/careers/**", "/api/study-plans/**", "/api/subjects/**")
                        .authenticated()
                        // ...but only ADMIN can create, update or delete them
                        .requestMatchers("/api/careers/**", "/api/study-plans/**", "/api/subjects/**")
                        .hasRole("ADMIN")

                        // Epics 1 and 4: only ADMIN manages students and teachers
                        .requestMatchers("/api/students/**", "/api/teachers/**").hasRole("ADMIN")

                        // Anything else: at least logged in
                        .anyRequest().authenticated()
                )

                // Validate the JWT from Keycloak and read its roles with our converter
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRolesConverter())));

        return http.build();
    }

    // Keycloak sends the roles inside the token like this:
    //   "realm_access": { "roles": ["ADMIN", "default-roles-siga-ipt", ...] }
    // Spring's hasRole("ADMIN") looks for an authority called "ROLE_ADMIN",
    // so we read that list and add the "ROLE_" prefix to each role.
    @SuppressWarnings("unchecked")
    private JwtAuthenticationConverter keycloakRolesConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || realmAccess.get("roles") == null) {
                return authorities;                       // token without roles -> no permissions
            }
            List<String> roles = (List<String>) realmAccess.get("roles");
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            return authorities;
        });
        return converter;
    }
}