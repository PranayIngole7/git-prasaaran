package com.pranay.gitprasaaran;

import com.pranay.gitprasaaran.infrastructure.github.GitHubProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GitHubProperties.class)
public class GitPrasaaranBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitPrasaaranBackendApplication.class, args);
    }
}
