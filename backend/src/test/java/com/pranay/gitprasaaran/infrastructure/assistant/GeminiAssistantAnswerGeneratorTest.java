package com.pranay.gitprasaaran.infrastructure.assistant;

import com.pranay.gitprasaaran.application.assistant.AssistantUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GeminiAssistantAnswerGeneratorTest {

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com";

    private static final String MODEL = "gemini-2.5-flash";

    @Test
    void shouldExtractAnswerFromGeminiResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        GeminiAssistantAnswerGenerator generator =
                new GeminiAssistantAnswerGenerator(
                        builder, BASE_URL, "test-api-key", MODEL
                );

        server.expect(requestTo(
                        BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"
                ))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {"text": "The repository "},
                                  {"text": "contains documentation."}
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String answer = generator.generate(
                "What does this repository contain?",
                "README.md: Project documentation"
        );

        assertEquals(
                "The repository contains documentation.",
                answer
        );
        assertEquals(MODEL, generator.modelName());

        server.verify();
    }

    @Test
    void shouldRejectMissingApiKeyWithoutMakingARequest() {
        GeminiAssistantAnswerGenerator generator =
                new GeminiAssistantAnswerGenerator(
                        RestClient.builder(), BASE_URL, "  ", MODEL
                );

        assertThrows(
                AssistantUnavailableException.class,
                () -> generator.generate("Question?", "Documentation")
        );
    }

    @Test
    void shouldTranslateProviderFailureIntoAssistantUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        GeminiAssistantAnswerGenerator generator =
                new GeminiAssistantAnswerGenerator(
                        builder, BASE_URL, "test-api-key", MODEL
                );

        server.expect(requestTo(
                        BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"
                ))
                .andRespond(withServerError());

        assertThrows(
                AssistantUnavailableException.class,
                () -> generator.generate("Question?", "Documentation")
        );

        server.verify();
    }

    @Test
    void shouldTranslateProviderAuthenticationFailureIntoAssistantUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        GeminiAssistantAnswerGenerator generator =
                new GeminiAssistantAnswerGenerator(
                        builder, BASE_URL, "test-api-key", MODEL
                );

        server.expect(requestTo(
                        BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"
                ))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        AssistantUnavailableException exception = assertThrows(
                AssistantUnavailableException.class,
                () -> generator.generate("Question?", "Documentation")
        );

        HttpClientErrorException cause =
                assertInstanceOf(HttpClientErrorException.class, exception.getCause());
        assertEquals(HttpStatus.UNAUTHORIZED, cause.getStatusCode());
        server.verify();
    }

    @Test
    void shouldTranslateProviderTimeoutIntoAssistantUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        GeminiAssistantAnswerGenerator generator =
                new GeminiAssistantAnswerGenerator(
                        builder, BASE_URL, "test-api-key", MODEL
                );

        server.expect(requestTo(
                        BASE_URL + "/v1beta/models/" + MODEL + ":generateContent"
                ))
                .andRespond(withException(new SocketTimeoutException("read timed out")));

        AssistantUnavailableException exception = assertThrows(
                AssistantUnavailableException.class,
                () -> generator.generate("Question?", "Documentation")
        );

        assertInstanceOf(ResourceAccessException.class, exception.getCause());
        server.verify();
    }
}
