package com.ramy.bugreport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;

/**
 * Base OpenAPI metadata (title, description, version and tag list) shown in Swagger UI and in the generated spec.
 *
 * <p>The version is the Maven project version, copied into {@code application.yml} at build time.
 * The tag names must match the {@code @Tag} names used on the controllers.
 */
@Configuration
public class OpenApiConfig {

    private static final String DESCRIPTION = """
            REST API of the Bug Report System, a minimalistic Jira-like bug tracker.

            **Authentication.** The API uses a session cookie and CSRF protection:
            1. Call `GET /api/csrf` to get a CSRF token (this also sets the `JSESSIONID` cookie).
            2. Sign in with `POST /api/auth/login` (form fields `username` and `password`, where `username` is the email),
               sending the token in the `X-CSRF-TOKEN` header.
            3. Send the session cookie with every request, and the `X-CSRF-TOKEN` header with every `POST`, `PATCH` and `DELETE`.

            Only `GET /api/csrf` and `POST /api/accounts` (registration) are public. Missing sign-in answers 401,
            a missing role, ownership rule or CSRF token answers 403. Errors have the body `{"message": "..."}`.
            """;

    /** Builds the API info and the tags, one per resource group. */
    @Bean
    public OpenAPI openApi(@Value("${app.version}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("Bug Report System API")
                        .description(DESCRIPTION)
                        .version(version))
                .addTagsItem(new Tag().name("Authentication").description("Current session, CSRF token, login and logout."))
                .addTagsItem(new Tag().name("Bug reports").description("Create, read, update and resolve bug reports."))
                .addTagsItem(new Tag().name("Comments").description("Discussion on bug reports."))
                .addTagsItem(new Tag().name("Projects").description("Software projects that bug reports belong to."))
                .addTagsItem(new Tag().name("Components").description("Parts of a project, each with a responsible developer."))
                .addTagsItem(new Tag().name("Accounts").description("User accounts and their roles."));
    }

    /**
     * Adds {@code POST /api/auth/login} and {@code POST /api/auth/logout} to the spec.
     *
     * <p>Spring Security handles both in {@code SecurityConfig} (there is no controller), so springdoc cannot find them on its own.
     */
    @Bean
    public OpenApiCustomizer authenticationEndpointsCustomizer() {
        return openApi -> {
            openApi.path("/api/auth/login", new PathItem().post(loginOperation()));
            openApi.path("/api/auth/logout", new PathItem().post(logoutOperation()));
        };
    }

    private Operation loginOperation() {
        Schema<?> form = new ObjectSchema()
                .addProperty("username", new StringSchema().description("Email address of the account.").example("admin@bugreport.local"))
                .addProperty("password", new StringSchema().description("Plain-text password.").example("Admin123!"))
                .addRequiredItem("username")
                .addRequiredItem("password");

        return new Operation()
                .addTagsItem("Authentication")
                .operationId("login")
                .summary("Sign in")
                .description("Signs in with form fields and starts a session (`JSESSIONID` cookie). Handled by Spring Security, not by a controller. "
                        + "Send the CSRF token from `GET /api/csrf` in the `X-CSRF-TOKEN` header. Access: public.")
                .requestBody(new RequestBody().required(true).content(new Content()
                        .addMediaType("application/x-www-form-urlencoded", new MediaType().schema(form))))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("Signed in; the session cookie is set. The body is empty."))
                        .addApiResponse("401", new ApiResponse().description("Wrong email or password. The body is empty."))
                        .addApiResponse("403", errorResponse("The CSRF token is missing or invalid.")));
    }

    private Operation logoutOperation() {
        return new Operation()
                .addTagsItem("Authentication")
                .operationId("logout")
                .summary("Sign out")
                .description("Ends the session. Handled by Spring Security, not by a controller. "
                        + "Send the CSRF token from `GET /api/csrf` in the `X-CSRF-TOKEN` header.")
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("Signed out. The body is empty."))
                        .addApiResponse("403", errorResponse("The CSRF token is missing or invalid.")));
    }

    private ApiResponse errorResponse(String description) {
        Schema<?> error = new Schema<>().$ref("#/components/schemas/ApiErrorResponse");
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json", new MediaType().schema(error)));
    }
}
