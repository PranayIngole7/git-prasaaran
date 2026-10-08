package com.pranay.gitprasaaran.api.repository;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RepositoryPatchRequest(
        @Size(max = 255) @Pattern(regexp = ".*\\S.*") String owner,
        @Size(max = 255) @Pattern(regexp = ".*\\S.*") String name,
        @Size(max = 255) @Pattern(regexp = ".*\\S.*") String branch,
        @Size(max = 500) @Pattern(regexp = ".*\\S.*") String contentPath,
        Boolean active
) {

    @AssertTrue(message = "At least one repository field must be provided")
    public boolean hasUpdates() {
        return owner != null
                || name != null
                || branch != null
                || contentPath != null
                || active != null;
    }
}
