package com.everypicfound.security.interfaces.web.probe;

import com.everypicfound.common.response.Result;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/search/_auth")
public class MediaAuthProbeController {

    @GetMapping("/probe")
    public Result<Map<String, Object>> probe(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        return Result.success(Map.of(
                "service", "media-search-service",
                "authenticated", authentication.isAuthenticated(),
                "subject", authentication.getName(),
                "authorities", authorities), "media-auth-probe");
    }
}
