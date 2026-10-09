package com.pranay.gitprasaaran.application.assistant;

public interface AssistantAnswerGenerator {

    String generate(String question, String documentationContext);

    default String modelName() {
        return "configured-provider";
    }
}
