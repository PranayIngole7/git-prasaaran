package com.pranay.gitprasaaran.infrastructure.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class GitHubWebhookSignatureVerifier {

    private static final String SIGNATURE_PREFIX = "sha256=";

    private final String webhookSecret;

    public GitHubWebhookSignatureVerifier(
            @Value("${gitprasaaran.github.webhook-secret:${GITHUB_WEBHOOK_SECRET:}}") String webhookSecret
    ) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isValid(String payload, String signature) {
        if (payload == null || signature == null || signature.isBlank()) {
            return false;
        }
        return isValid(payload.getBytes(StandardCharsets.UTF_8), signature);
    }

    public boolean isValid(byte[] payload, String signature) {
        if (payload == null || signature == null || signature.isBlank()) {
            return false;
        }
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }

        String providedSignature = signature.trim();
        if (!providedSignature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        String expectedHex = providedSignature.substring(SIGNATURE_PREFIX.length());
        if (expectedHex.isBlank() || !expectedHex.chars().allMatch(ch -> Character.digit(ch, 16) != -1)) {
            return false;
        }

        String actualSignature = calculateSignature(payload);
        return MessageDigest.isEqual(
                actualSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String calculateSignature(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload);
            return SIGNATURE_PREFIX + HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            return SIGNATURE_PREFIX;
        }
    }
}
