package com.tenzin.splitter.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui.html. Log in with POST /api/auth/login, then paste the
 * accessToken into "Authorize" to call the protected endpoints.
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
		info = @Info(title = "ExSplit API", version = "v1",
				description = "Split shared expenses in groups and settle up with the fewest payments. "
						+ "Amounts are in paise (1 rupee = 100 paise)."),
		security = @SecurityRequirement(name = OpenApiConfig.BEARER))
@SecurityScheme(name = OpenApiConfig.BEARER, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

	static final String BEARER = "bearerAuth";

}
