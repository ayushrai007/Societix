package com.societix.platform.society.internal;

import com.societix.platform.society.api.CreateSocietyRequest;
import com.societix.platform.society.data.SocietyRepository;
import com.societix.platform.society.domain.Society;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocietyOnboardingService {

    private final SocietyRepository societies;
    private final EntityManager entityManager;

    public SocietyOnboardingService(SocietyRepository societies, EntityManager entityManager) {
        this.societies = societies;
        this.entityManager = entityManager;
    }

    @Transactional
    public Society onboard(CreateSocietyRequest request) {
        UUID id = UUID.randomUUID();
        // The tenant registry is itself protected by RLS. During onboarding the new
        // tenant is the only valid context for the row being inserted.
        entityManager.createNativeQuery("select set_config('app.tenant_id', :tenantId, true)")
                .setParameter("tenantId", id.toString())
                .getSingleResult();
        return societies.save(new Society(id, request.name(), request.address(), request.state()));
    }
}
