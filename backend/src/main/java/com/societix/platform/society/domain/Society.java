package com.societix.platform.society.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "society")
public class Society {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", nullable = false, length = 1000)
    private String address;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    protected Society() {
        // Required by JPA.
    }

    public Society(UUID id, String name, String address, String state) {
        this.id = id;
        this.tenantId = id;
        this.name = name;
        this.address = address;
        this.state = state;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getState() {
        return state;
    }
}
