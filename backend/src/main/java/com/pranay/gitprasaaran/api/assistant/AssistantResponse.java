package com.pranay.gitprasaaran.api.assistant;

import java.util.List;

public record AssistantResponse(
        String answer,
        List<String> sources,
        String model
) {
    public AssistantResponse {
        sources = List.copyOf(sources);
    }
}
