package com.pranay.gitprasaaran.api.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PrivateDocumentRequest(
        @NotBlank
        @Size(max = 200)
        String title,
        @NotBlank
        @Size(max = 120)
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*")
        String slug,
        @NotBlank
        @Size(max = 100_000)
        String content) {
}
