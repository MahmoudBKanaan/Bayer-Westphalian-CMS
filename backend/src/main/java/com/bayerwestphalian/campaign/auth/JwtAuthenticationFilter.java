package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.common.api.SecureErrorResponses;
import com.bayerwestphalian.campaign.common.exception.UnauthorizedException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SecureErrorResponses secureErrorResponses;

    public JwtAuthenticationFilter(
            JwtService jwtService, SecureErrorResponses secureErrorResponses) {
        this.jwtService = jwtService;
        this.secureErrorResponses = secureErrorResponses;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        try {
            if (StringUtils.hasText(authorizationHeader)
                    && authorizationHeader.startsWith("Bearer ")) {
                authenticate(authorizationHeader.substring(7));
            }
        } catch (UnauthorizedException exception) {
            SecurityContextHolder.clearContext();
            // Item 538: structured JSON only — do not echo exception message (may leak token details).
            secureErrorResponses.write(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Authentication is required");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new UnauthorizedException("Bearer access token is required");
        }

        JwtTokenClaims claims = jwtService.validateToken(accessToken, JwtTokenType.ACCESS);
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(claims.userId(), claims.email(), claims.roles());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, accessToken, authorities(claims.roles()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private List<SimpleGrantedAuthority> authorities(List<SystemRoleName> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }
}
