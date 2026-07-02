package com.openbake.server.repository;

import com.openbake.server.entity.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppSettingsRepository extends JpaRepository<AppSettings, String> {
    Optional<AppSettings> findByKey(String key);
}
