package com.pranay.gitprasaaran.application.assistant;

import com.pranay.gitprasaaran.api.assistant.AssistantRequest;
import com.pranay.gitprasaaran.api.assistant.AssistantResponse;
import com.pranay.gitprasaaran.application.document.DocumentService;
import com.pranay.gitprasaaran.application.repository.RepositoryService;
import com.pranay.gitprasaaran.domain.document.Document;
import com.pranay.gitprasaaran.domain.repository.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class AssistantService {

    private static final int MAX_DOCUMENTS = 6;
    private static final int MAX_DOCUMENT_CHARS = 2500;
    private static final int MAX_CONTEXT_CHARS = 12000;

    private final RepositoryService repositoryService;
    private final DocumentService documentService;
    private final AssistantAnswerGenerator answerGenerator;

    public AssistantService(
            RepositoryService repositoryService,
            DocumentService documentService,
            AssistantAnswerGenerator answerGenerator
    ) {
        this.repositoryService = repositoryService;
        this.documentService = documentService;
        this.answerGenerator = answerGenerator;
    }

    public AssistantResponse ask(AssistantRequest request) {
        Repository repository = repositoryService.findById(request.repositoryId());

        if (!repository.active()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "The selected repository is inactive."
            );
        }

        List<Document> documents = documentService.findAll(repository);
        StringBuilder context = new StringBuilder();
        List<String> sources = new ArrayList<>();

        for (Document document : documents) {
            if (sources.size() >= MAX_DOCUMENTS
                    || context.length() >= MAX_CONTEXT_CHARS) {
                break;
            }

            String content = document.content();
            if (content == null || content.isBlank()) {
                continue;
            }

            String sourcePath = document.sourcePath();
            if (sourcePath == null || sourcePath.isBlank()) {
                sourcePath = document.slug();
            }
            if (sourcePath == null || sourcePath.isBlank()) {
                sourcePath = "unknown";
            }

            String header = "\n\n--- SOURCE: " + sourcePath + " ---\n";
            int remaining = MAX_CONTEXT_CHARS - context.length();
            if (header.length() >= remaining) {
                break;
            }

            int contentLimit = Math.min(MAX_DOCUMENT_CHARS, content.length());
            int allowed = Math.min(contentLimit, remaining - header.length());
            if (allowed <= 0) {
                break;
            }

            context.append(header).append(content, 0, allowed);
            sources.add(sourcePath);
        }

        if (sources.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "No usable Markdown documentation was found in this repository."
            );
        }

        String answer = answerGenerator.generate(
                request.question().trim(),
                context.toString()
        );

        return new AssistantResponse(
                answer,
                sources,
                answerGenerator.modelName()
        );
    }
}
