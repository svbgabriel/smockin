package com.smockin.config;

import com.smockin.admin.persistence.CoreDataHandler;
import com.smockin.admin.service.MockedServerEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class StartUpConfig {

    private final CoreDataHandler coreDataHandler;
    private final MockedServerEngineService mockedServerEngineService;

    public StartUpConfig(CoreDataHandler coreDataHandler, MockedServerEngineService mockedServerEngineService) {
        this.coreDataHandler = coreDataHandler;
        this.mockedServerEngineService = mockedServerEngineService;
    }

    private final Logger logger = LoggerFactory.getLogger(StartUpConfig.class);

    @PostConstruct
    public void after() {

        logger.info("Start up config");

        coreDataHandler.exec();

        try {
            mockedServerEngineService.handleServerAutoStart();
        } catch (Exception ex) {
            logger.error("Error auto starting mock servers ", ex);
        }

    }
}
