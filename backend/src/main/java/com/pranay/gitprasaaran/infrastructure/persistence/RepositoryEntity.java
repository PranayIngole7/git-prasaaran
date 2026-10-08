package com.pranay.gitprasaaran.infrastructure.persistence;

import com.pranay.gitprasaaran.domain.repository.Repository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "repositories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_repositories_owner_name",
                columnNames = {"owner", "name"}
        )
)
public class RepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner", nullable = false, length = 255)
    private String owner;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "branch", nullable = false, length = 255)
    private String branch;

    @Column(name = "content_path", nullable = false, length = 500)
    private String contentPath;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RepositoryEntity() {
    }

    public RepositoryEntity(Repository repository) {
        this.id = repository.id();
        this.owner = repository.owner();
        this.name = repository.name();
        this.branch = repository.branch();
        this.contentPath = repository.contentPath();
        this.active = repository.active();
        this.createdAt = repository.createdAt();
        this.updatedAt = repository.updatedAt();
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Repository toDomain() {
        return new Repository(id, owner, name, branch, contentPath, active, createdAt, updatedAt);
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getBranch() {
        return branch;
    }

    public String getContentPath() {
        return contentPath;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
