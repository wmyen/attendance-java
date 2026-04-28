package com.attendance.security;

import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String role;
    private final boolean mustChangePassword;

    public CustomUserDetails(Long id, String email, String password, String role, boolean mustChangePassword) {
        this.id = Objects.requireNonNull(id);
        this.email = email;
        this.password = password;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }

    @NonNull
    public Long getId() { return Objects.requireNonNull(id); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
