package com.pranay.gitprasaaran.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private Role name;

    protected RoleEntity() {
    }

    public RoleEntity(Role name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Role getName() {
        return name;
    }
}