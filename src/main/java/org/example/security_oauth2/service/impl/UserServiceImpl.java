package org.example.security_oauth2.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security_oauth2.Dto.UserDto;
import org.example.security_oauth2.model.Role;
import org.example.security_oauth2.model.User;
import org.example.security_oauth2.repository.UserRepository;
import org.example.security_oauth2.service.UserService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate =
                new DefaultOAuth2UserService();

        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        Object idObj = oAuth2User.getAttribute("id");
        Long githubId = null;
        if (idObj instanceof Number) {
            githubId = ((Number) idObj).longValue();
        } else {
            throw new OAuth2AuthenticationException("Unable to retrieve GitHub ID");
        }

        String login = oAuth2User.getAttribute("login");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (name == null) {
            name = login;
        }

        if (email == null) {
            email = fetchEmailFromGithub(userRequest);
        }

        if (email == null) {
            log.error("Email is missing; cannot create user.");
            throw new OAuth2AuthenticationException("Insufficient data for registration.");
        }

        Optional<User> userOptional = userRepository.findByGithubId(githubId);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            log.info("User {} found in database", email);

            if (user.getName() == null || user.getName().isEmpty()) {
                user.setName(name);
            }
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                user.setEmail(email);
            }
            userRepository.save(user);
        } else {
            log.info("Creating new user: {}", email);

            user = User.builder()
                    .githubId(githubId)
                    .name(name)
                    .password("")
                    .email(email)
                    .role(Role.ROLE_USER)
                    .build();

            userRepository.save(user);
            log.info("New user created: {}", email);
        }

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("name", user.getName());
        attributes.put("email", user.getEmail());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getAuthority())),
                attributes,
                "login"
        );
    }

    public UserDto buildUserProfile(OAuth2User principal) {
        Object idObj = principal.getAttribute("id");
        Long githubId = null;
        if (idObj instanceof Number) {
            githubId = ((Number) idObj).longValue();
        }

        String login = principal.getAttribute("login");

        Optional<User> userOptional = userRepository.findByGithubId(githubId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            return UserDto.builder()
                    .name(user.getName())
                    .login(login)
                    .id(user.getId())
                    .email(user.getEmail())
                    .build();
        } else {
            String name = principal.getAttribute("name");
            String email = principal.getAttribute("email");

            return UserDto.builder()
                    .name(name)
                    .login(login)
                    .id(githubId)
                    .email(email)
                    .build();
        }
    }

    private String fetchEmailFromGithub(OAuth2UserRequest userRequest) {
        String emailEndpoint = "https://api.github.com/user/emails";
        String accessToken = userRequest.getAccessToken().getTokenValue();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                emailEndpoint,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        List<Map<String, Object>> emails = response.getBody();
        log.debug("Emails from GitHub: {}", emails);

        if (emails != null) {
            for (Map<String, Object> emailRecord : emails) {
                Boolean primary = (Boolean) emailRecord.get("primary");
                Boolean verified = (Boolean) emailRecord.get("verified");
                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                    return (String) emailRecord.get("email");
                }
            }
        }
        return null;
    }
}
