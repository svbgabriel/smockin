package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mgallina.
 */
public interface AppConfigDAO extends JpaRepository<AppConfig, Long> {

}
