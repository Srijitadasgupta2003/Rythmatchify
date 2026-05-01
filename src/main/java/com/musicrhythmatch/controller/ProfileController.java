package com.musicrhythmatch.controller;

import com.musicrhythmatch.entity.User;
import com.musicrhythmatch.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import java.util.Optional;

@Controller
@Slf4j
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        String spotifyId = ((OAuth2AuthenticationToken) principal).getName();
        Optional<User> userOpt = userService.getUserById(spotifyId);

        userOpt.ifPresentOrElse(
                user -> model.addAttribute("user", user),
                ()   -> model.addAttribute("errorMsg", "Profile not found.")
        );

        return "profile";
    }

    /**
     * Deletes all user data from the database, then logs them out.
     * This satisfies the Spotify ToS requirement for a "Delete My Data" feature.
     */
    @PostMapping("/profile/delete")
    public String deleteData(Principal principal, HttpServletRequest request) throws ServletException {
        String spotifyId = ((OAuth2AuthenticationToken) principal).getName();
        log.info("User {} requested data deletion.", spotifyId);

        userService.deleteUser(spotifyId);

        // Invalidate the HTTP session and clear the security context
        request.logout();

        return "redirect:/?deleted=true";
    }
}