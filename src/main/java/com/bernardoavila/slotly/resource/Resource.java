package com.bernardoavila.slotly.resource;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Resource() {
    }

    public Resource(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }

    public void rename(String name, String description) {
        this.name = name;
        this.description = description;
    }
}