package com.pranay.gitprasaaran.application.webhook;

public record GitHubPushEvent(String ref, boolean deleted, String payload) {
}
