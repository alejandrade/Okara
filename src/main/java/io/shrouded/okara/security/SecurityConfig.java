package io.shrouded.okara.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/info").permitAll()
                .requestMatchers("/api/feed/main").permitAll()
                .requestMatchers("/api/feed/{postId}").permitAll()
                .requestMatchers("/api/feed/{postId}/comments").permitAll()
                .requestMatchers("/api/feed/user/{userId}").permitAll()
                .requestMatchers("/api/auth/users/{username}").permitAll()
                
                // Protected endpoints - require authentication
                .requestMatchers("/api/auth/login").authenticated()
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/auth/follow/**").authenticated()
                .requestMatchers("/api/auth/unfollow/**").authenticated()
                .requestMatchers("/api/auth/profile").authenticated()
                .requestMatchers("/api/feed/post").authenticated()
                .requestMatchers("/api/feed/*/comment").authenticated()
                .requestMatchers("/api/feed/*/like").authenticated()
                .requestMatchers("/api/feed/*/dislike").authenticated()
                .requestMatchers("/api/feed/*/retweet").authenticated()
                .requestMatchers("/api/feed/*/quote").authenticated()
                .requestMatchers("/api/feed/timeline").authenticated()
                .requestMatchers("/api/feed/*/delete").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
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