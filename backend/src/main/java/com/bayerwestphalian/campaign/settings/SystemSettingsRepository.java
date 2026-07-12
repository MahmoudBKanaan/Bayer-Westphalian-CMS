package com.bayerwestphalian.campaign.settings;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, UUID> {}
