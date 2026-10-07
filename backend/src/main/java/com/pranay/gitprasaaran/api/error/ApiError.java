package com.pranay.gitprasaaran.api.error;

public record ApiError(
        String code,
        String message
) {
}
