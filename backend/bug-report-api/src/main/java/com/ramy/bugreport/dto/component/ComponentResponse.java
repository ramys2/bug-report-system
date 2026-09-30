package com.ramy.bugreport.dto.component;

import java.util.UUID;

import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.domain.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ramy.bugreport.openapi.ApiExamples;

/**
 * A component in {@code GET /api/components}.
 *
 * @param id component id
 * @param name display name
 * @param description description; may be null
 * @param responsibleUserName display name of the responsible user; null if that user cannot be found
 */
@Schema(description = "A component in `GET /api/components`.")
public record ComponentResponse(
        @Schema(description = "Component id.", example = ApiExamples.UUID, requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Display name.", example = "Backend API", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Description. May be null.", example = "REST API and persistence layer.")
        String description,
        @Schema(description = "Display name of the responsible user. Null if that user cannot be found.", example = "Daniel Developer")
        String responsibleUserName
) {

    public static ComponentResponse from(Component component, UserAccount responsibleUser) {
        return new ComponentResponse(
                component.getId(),
                component.getName(),
                component.getDescription(),
                responsibleUser == null ? null : responsibleUser.getName());
    }
}
