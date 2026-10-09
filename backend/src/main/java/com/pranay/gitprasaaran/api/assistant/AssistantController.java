package com.pranay.gitprasaaran.api.assistant;

import com.pranay.gitprasaaran.application.assistant.AssistantService;
import com.pranay.gitprasaaran.application.assistant.AssistantUnavailableException;
import com.pranay.gitprasaaran.api.error.ApiError;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/ask")
    public AssistantResponse ask(@Valid @RequestBody AssistantRequest request) {
        return assistantService.ask(request);
    }

    @ExceptionHandler(AssistantUnavailableException.class)
    public ResponseEntity<ApiError> handleAssistantUnavailable(
            AssistantUnavailableException ex
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError(
                        "ASSISTANT_UNAVAILABLE",
                        "The AI assistant is temporarily unavailable. Check the configured provider."
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleRequestFailure(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String code = status == HttpStatus.UNPROCESSABLE_ENTITY
                ? "DOCUMENTATION_UNAVAILABLE"
                : "ASSISTANT_REQUEST_REJECTED";

        return ResponseEntity.status(status)
                .body(new ApiError(code, ex.getReason() == null
                        ? "The assistant request could not be completed."
                        : ex.getReason()));
    }
}
