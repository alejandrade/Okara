package io.shrouded.okara.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final FirebaseReactiveAuthenticationWebFilter firebaseReactiveAuthenticationWebFilter;
    
    @Value("${docs.security.username:admin}")
    private String docsUsername;
    
    @Value("${docs.security.password:docs123}")
    private String docsPassword;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/info").permitAll()
                        // API Documentation endpoints - basic auth required
                        .pathMatchers("/v3/api-docs/**").hasRole("DOCS")
                        .pathMatchers("/swagger-ui/**").hasRole("DOCS")
                        .pathMatchers("/swagger-ui.html").hasRole("DOCS")
                        .pathMatchers("/api/feed/{postId}").authenticated()
                        .pathMatchers("/api/feed/{postId}/comments").authenticated()
                        .pathMatchers("/api/feed/user/{userId}").authenticated()
                        .pathMatchers("/api/auth/users/{username}").authenticated()
                        .pathMatchers("/api/feed/main").authenticated()
                        .pathMatchers("/api/auth/login").authenticated()
                        .pathMatchers("/api/auth/me").authenticated()
                        .pathMatchers("/api/auth/follow/**").authenticated()
                        .pathMatchers("/api/auth/unfollow/**").authenticated()
                        .pathMatchers("/api/auth/profile").authenticated()
                        .pathMatchers("/api/feed/post").authenticated()
                        .pathMatchers("/api/feed/*/comment").authenticated()
                        .pathMatchers("/api/feed/*/like").authenticated()
                        .pathMatchers("/api/feed/*/dislike").authenticated()
                        .pathMatchers("/api/feed/*/retweet").authenticated()
                        .pathMatchers("/api/feed/*/quote").authenticated()
                        .pathMatchers("/api/feed/timeline").authenticated()
                        .pathMatchers("/api/feed/*/delete").authenticated()

                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                .httpBasic(httpBasic -> {})
                .addFilterBefore(firebaseReactiveAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails docsUser = User.builder()
                .username(docsUsername)
                .password(passwordEncoder().encode(docsPassword))
                .roles("DOCS")
                .build();
        return new MapReactiveUserDetailsService(docsUser);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}