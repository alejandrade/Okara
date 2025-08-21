package io.shrouded.okara.security;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.shrouded.okara.service.FirebaseAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseReactiveAuthenticationWebFilter implements WebFilter {

    private final FirebaseAuthService firebaseAuthService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        log.info("🔥 REACTIVE FILTER EXECUTING - Request: {} {}",
                 exchange.getRequest().getMethod(), exchange.getRequest().getPath().value());
        log.info("🔥 Authorization header: {}", authHeader != null ? "PRESENT" : "NULL");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String idToken = authHeader.substring(7);
            log.info("🔥 Found Authorization header, token length: {}", idToken.length());

            return Mono.fromCallable(() -> {
                           try {
                               log.info("🔥 Verifying Firebase token...");
                               FirebaseToken decodedToken = firebaseAuthService.verifyToken(idToken);

                               String firebaseUid = decodedToken.getUid();
                               String email = decodedToken.getEmail();
                               String name = decodedToken.getName();

                               log.info("🔥 Token verified successfully - UID: {}, Email: {}, Name: {}", firebaseUid, email, name);

                               if (firebaseUid == null || firebaseUid.isBlank()) {
                                   log.error("🔥 Firebase UID is null/blank - cannot authenticate");
                                   return null;
                               }

                               List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

                               return new FirebaseAuthenticationToken(
                                       firebaseUid, name, authorities);

                           } catch (FirebaseAuthException e) {
                               log.error("🔥 Invalid Firebase token", e);
                               return null;
                           } catch (Exception e) {
                               log.error("🔥 Unexpected error in Firebase filter", e);
                               return null;
                           }
                       })
                       .flatMap(authToken -> {
                           if (authToken == null) {
                               // Authentication failed, continue without auth
                               return chain.filter(exchange);
                           }

                           // Authentication successful, set context and continue
                           SecurityContext context = new SecurityContextImpl(authToken);
                           return chain.filter(exchange)
                                       .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
                       });
        }

        log.info("🔥 No valid Authorization header, continuing without authentication");
        return chain.filter(exchange);
    }
}