package com.musicrhythmatch.controller;

import com.musicrhythmatch.dto.MatchResult;
import com.musicrhythmatch.entity.User;
import com.musicrhythmatch.service.MatchingEngineService;
import com.musicrhythmatch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@Slf4j
public class DashboardController {

    private final UserService userService;
    private final MatchingEngineService matchingEngineService;

    public DashboardController(UserService userService,
                               MatchingEngineService matchingEngineService) {
        this.userService = userService;
        this.matchingEngineService = matchingEngineService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) principal;
        String spotifyId = oauthToken.getName();

        Optional<User> userOpt = userService.getUserById(spotifyId);
        if (userOpt.isEmpty()) {
            // Edge case: auth succeeded but DB save failed
            log.warn("User {} authenticated but not found in DB.", spotifyId);
            model.addAttribute("errorMsg", "Your profile is still loading. Please wait a moment and refresh.");
            return "dashboard";
        }

        User currentUser = userOpt.get();
        List<User> pool = userService.getAllUsersExcept(spotifyId);

        List<MatchResult> soulmates = Collections.emptyList();
        List<MatchResult> nemeses   = Collections.emptyList();

        if (!pool.isEmpty()) {
            soulmates = matchingEngineService.findSoulmates(currentUser, pool);
            nemeses   = matchingEngineService.findNemeses(currentUser, pool);
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("soulmates", soulmates);
        model.addAttribute("nemeses", nemeses);
        model.addAttribute("hasMatches", !pool.isEmpty());
        model.addAttribute("totalUsers", pool.size() + 1); // +1 for current user

        return "dashboard";
    }
}