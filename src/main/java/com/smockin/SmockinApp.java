package com.smockin;

import com.smockin.admin.persistence.CoreDataHandler;
import com.smockin.admin.service.MockedServerEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import javax.annotation.PostConstruct;

/**
 * Created by mgallina.
 */
@SpringBootApplication
@ComponentScan({ "com.smockin.admin", "com.smockin.mockserver" })
@EnableJpaRepositories("com.smockin.admin.persistence.dao")
@EntityScan("com.smockin.admin.persistence.entity")
class SmockinConfig {

    private final CoreDataHandler coreDataHandler;
    private final MockedServerEngineService mockedServerEngineService;

    public SmockinConfig(CoreDataHandler coreDataHandler, MockedServerEngineService mockedServerEngineService) {
        this.coreDataHandler = coreDataHandler;
        this.mockedServerEngineService = mockedServerEngineService;
    }

    private final Logger logger = LoggerFactory.getLogger(SmockinConfig.class);

    @PostConstruct
    public void after() {

        coreDataHandler.exec();

        try {
            mockedServerEngineService.handleServerAutoStart();
        } catch (Exception ex) {
            logger.error("Error auto starting mock servers ", ex);
        }

    }

}

public class SmockinApp {

    public static void main(String[] args) {
        SpringApplication.run(SmockinConfig.class, args);
    }

}
