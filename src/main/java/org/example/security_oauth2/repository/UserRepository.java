package org.example.security_oauth2.repository;

import org.example.security_oauth2.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGithubId(Long githubId);
    Optional<User> findByName(String name);

}
