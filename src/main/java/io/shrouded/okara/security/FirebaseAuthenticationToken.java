package io.shrouded.okara.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {

    private final String firebaseUid;
    private final String email;
    private final String name;

    public FirebaseAuthenticationToken(String firebaseUid, String email, String name,
                                       Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.name = name;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return firebaseUid;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }
}