package com.smockin.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.vaadin.flow.spring.annotation.EnableVaadin;

@SpringBootApplication
@ComponentScan({ "com.smockin.admin", "com.smockin.mockserver", "com.smockin.config" })
@EnableJpaRepositories("com.smockin.admin.persistence.dao")
@EntityScan("com.smockin.admin.persistence.entity")
@EnableVaadin({"com.smockin.admin.ui"})
public class SmockinConfig {}
