package org.example.security_oauth2.controller;

import lombok.RequiredArgsConstructor;

import org.example.security_oauth2.Dto.UserDto;
import org.example.security_oauth2.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/user/profile")
    public ResponseEntity<UserDto> getUserProfile(@AuthenticationPrincipal OAuth2User principal) {
        UserDto userProfile = userService.buildUserProfile(principal);
        if (userProfile == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userProfile);
    }
}
