package org.example.security_oauth2.service;

import org.example.security_oauth2.Dto.UserDto;
import org.example.security_oauth2.model.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByEmail(String email);
    User save(User user);
    UserDto buildUserProfile(OAuth2User principal);
}
