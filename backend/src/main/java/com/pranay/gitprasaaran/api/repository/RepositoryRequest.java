package com.pranay.gitprasaaran.api.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RepositoryRequest(
        @NotBlank @Size(max = 255) String owner,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String branch,
        @NotBlank @Size(max = 500) String contentPath
) {
}
