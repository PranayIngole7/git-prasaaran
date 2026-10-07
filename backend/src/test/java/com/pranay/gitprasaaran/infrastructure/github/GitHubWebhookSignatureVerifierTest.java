package com.pranay.gitprasaaran.infrastructure.github;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubWebhookSignatureVerifierTest {

    @Test
    void shouldAcceptValidSignature() throws Exception {
        String payload = "{\"hello\":\"world\"}";
        String secret = "super-secret";
        GitHubWebhookSignatureVerifier verifier = new GitHubWebhookSignatureVerifier(secret);

        String signature = sign(payload, secret);

        assertTrue(verifier.isValid(payload, signature));
    }

    @Test
    void shouldRejectInvalidSignature() throws Exception {
        String payload = "{\"hello\":\"world\"}";
        String secret = "super-secret";
        GitHubWebhookSignatureVerifier verifier = new GitHubWebhookSignatureVerifier(secret);

        assertFalse(verifier.isValid(payload, "sha256=def456"));
    }

    @Test
    void shouldRejectMissingSignature() {
        GitHubWebhookSignatureVerifier verifier = new GitHubWebhookSignatureVerifier("super-secret");

        assertFalse(verifier.isValid("{\"hello\":\"world\"}", null));
        assertFalse(verifier.isValid("{\"hello\":\"world\"}", "   "));
    }

    @Test
    void shouldRejectMalformedSignature() {
        GitHubWebhookSignatureVerifier verifier = new GitHubWebhookSignatureVerifier("super-secret");

        assertFalse(verifier.isValid("{\"hello\":\"world\"}", "sha256=not-hex"));
        assertFalse(verifier.isValid("{\"hello\":\"world\"}", "not-sha256"));
    }

    @Test
    void shouldRejectBlankSecret() {
        GitHubWebhookSignatureVerifier verifier = new GitHubWebhookSignatureVerifier("   ");

        assertFalse(verifier.isValid("{\"hello\":\"world\"}", "sha256=abc123"));
    }

    @Test
    void shouldRejectTamperedBody() throws Exception {
        String secret = "super-secret";
        String originalPayload = "{\"hello\":\"world\"}";
        String tamperedPayload = "{\"hello\":\"earth\"}";
        GitHubWebhookSignatureVerifier verifier = new GitHubWebhookSignatureVerifier(secret);

        String validSignature = sign(originalPayload, secret);

        assertFalse(verifier.isValid(tamperedPayload, validSignature));
    }

    private static String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return "sha256=" + HexFormat.of().formatHex(digest);
    }
}
