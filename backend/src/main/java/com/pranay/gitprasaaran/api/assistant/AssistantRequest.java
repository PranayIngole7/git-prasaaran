package com.pranay.gitprasaaran.api.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AssistantRequest(
        @NotNull @Positive Long repositoryId,
        @NotBlank @Size(max = 2000) String question
) {
}
