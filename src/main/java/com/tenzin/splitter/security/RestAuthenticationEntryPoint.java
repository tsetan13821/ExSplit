package com.tenzin.splitter.security;

import com.tenzin.splitter.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 401 for requests with no valid access token. Hands off to the MVC exception resolver
 * so the body is the same ProblemDetail JSON that {@code GlobalExceptionHandler} writes
 * for every other error.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final HandlerExceptionResolver resolver;

	public RestAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) {
		Object reason = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
		String detail = reason != null ? reason.toString() : "Authentication required";
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		resolver.resolveException(request, response, null, ApiException.unauthorized(detail));
	}

}
