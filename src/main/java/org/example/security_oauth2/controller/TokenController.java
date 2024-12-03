package org.example.security_oauth2.controller;

import lombok.RequiredArgsConstructor;
import org.example.security_oauth2.service.TokenRevocationService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class TokenController {

    private final TokenRevocationService tokenRevocationService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/user/revoke")
    public String revokeToken(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );
        tokenRevocationService.revokeToken(client.getAccessToken().getTokenValue());
        return "redirect:/";
    }
}
