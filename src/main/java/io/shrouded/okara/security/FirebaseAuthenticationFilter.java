package io.shrouded.okara.security;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.shrouded.okara.service.FirebaseAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    
    private final FirebaseAuthService firebaseAuthService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String idToken = authHeader.substring(7);
            
            try {
                FirebaseToken decodedToken = firebaseAuthService.verifyToken(idToken);
                
                String firebaseUid = decodedToken.getUid();
                String email = decodedToken.getEmail();
                String name = decodedToken.getName();
                
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                
                FirebaseAuthenticationToken authToken = new FirebaseAuthenticationToken(
                    firebaseUid, email, name, authorities);
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                log.debug("Successfully authenticated user: {}", email);
                
            } catch (FirebaseAuthException e) {
                log.debug("Invalid Firebase token: {}", e.getMessage());
                // Don't set authentication - let Spring Security handle it
            }
        }
        
        filterChain.doFilter(request, response);
    }
}