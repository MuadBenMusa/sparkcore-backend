package com.sparkcore.backend.repository;

import com.sparkcore.backend.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    // Findet einen Nutzer anhand seines Namens (wichtig für den Login!)
    Optional<AppUser> findByUsername(String username);
}