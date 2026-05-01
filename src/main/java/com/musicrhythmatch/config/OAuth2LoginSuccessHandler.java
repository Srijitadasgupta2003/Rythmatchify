package com.musicrhythmatch.config;

import com.musicrhythmatch.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Fires immediately after a successful Spotify OAuth2 login.
 * Grabs the access token, calls Spotify APIs, builds the taste vector,
 * and saves the user profile to Supabase before redirecting to /dashboard.
 */
@Component
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuth2LoginSuccessHandler(UserService userService,
                                     OAuth2AuthorizedClientService authorizedClientService) {
        this.userService = userService;
        this.authorizedClientService = authorizedClientService;
        setDefaultTargetUrl("/dashboard");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        // Retrieve the stored OAuth2 client (which holds the access token)
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            log.error("Could not retrieve OAuth2 authorized client for user: {}", oauthToken.getName());
            response.sendRedirect("/?error=auth_failed");
            return;
        }

        String accessToken = client.getAccessToken().getTokenValue();
        OAuth2User principal = oauthToken.getPrincipal();

        try {
            log.info("Fetching Spotify data for user: {}", principal.getAttribute("id"));
            userService.fetchAndSaveUserProfile(accessToken, principal);
            log.info("Successfully saved profile for: {}", principal.getAttribute("display_name"));
        } catch (Exception e) {
            // Don't crash the login — let the user in even if data fetch partially failed
            log.error("Error during Spotify data fetch for user {}: {}",
                    principal.getAttribute("id"), e.getMessage());
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}