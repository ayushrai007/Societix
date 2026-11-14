package com.societix.platform.society.data;

import com.societix.platform.society.domain.Society;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocietyRepository extends JpaRepository<Society, UUID> {}
