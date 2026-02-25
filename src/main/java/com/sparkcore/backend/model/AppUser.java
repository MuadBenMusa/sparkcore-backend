package com.sparkcore.backend.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "app_users")
public class AppUser implements UserDetails { // WICHTIG: implements UserDetails hinzugefügt!

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public AppUser() {}

    public AppUser(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // --- Die Methoden, die Spring Security ab jetzt zwingend verlangt ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Wandelt unsere Rolle (z.B. ADMIN) in ein Format um, das Spring versteht (ROLE_ADMIN)
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    // --- Unsere alten Getter und Setter für die ID und Rolle ---
    public Long getId() { return id; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
}