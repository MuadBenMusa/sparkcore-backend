package com.sparkcore.backend.config;

import com.sparkcore.backend.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationConfig {

    private final UserRepository userRepository;

    // Wir fordern NUR noch das UserRepository an.
    // Den PasswordEncoder bauen wir uns ab jetzt selbst!
    public ApplicationConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Passwörter mit BCrypt hashen
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // User aus der DB laden – wird von Spring Security beim Login aufgerufen
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User nicht gefunden"));
    }

    // UserDetailsService + PasswordEncoder zusammenstecken
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());

        // Wir rufen hier einfach unsere eigene Methode von oben auf!
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    // den AuthenticationManager aus der Spring-Konfiguration holen
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}