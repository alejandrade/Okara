package io.shrouded.okara.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
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
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/api/info").permitAll()
                .pathMatchers("/api/feed/main").permitAll()
                .pathMatchers("/api/feed/{postId}").permitAll()
                .pathMatchers("/api/feed/{postId}/comments").permitAll()
                .pathMatchers("/api/feed/user/{userId}").permitAll()
                .pathMatchers("/api/auth/users/{username}").permitAll()
                
                // Protected endpoints - require authentication
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
            .addFilterBefore(firebaseReactiveAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
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