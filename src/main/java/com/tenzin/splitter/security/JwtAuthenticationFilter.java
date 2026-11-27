package com.tenzin.splitter.security;

import java.io.IOException;
import java.util.List;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <token>}, verifies it and puts an {@link AuthUser}
 * into the SecurityContext. A missing or bad token leaves the context empty; Spring
 * Security then rejects protected routes with 401 via {@link RestAuthenticationEntryPoint}.
 *
 * Not a Spring bean on purpose: as a bean, Spring Boot would also register it as a plain
 * servlet filter outside the security chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	/** Request attribute telling the entry point why authentication failed. */
	static final String AUTH_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".error";

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			String token = header.substring(BEARER_PREFIX.length()).trim();
			try {
				AuthUser user = jwtService.verify(token);
				var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, List.of());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContext context = SecurityContextHolder.createEmptyContext();
				context.setAuthentication(authentication);
				SecurityContextHolder.setContext(context);
			}
			catch (ExpiredJwtException ex) {
				request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Access token expired");
			}
			catch (JwtException | IllegalArgumentException ex) {
				request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Invalid access token");
			}
		}
		chain.doFilter(request, response);
	}

}
