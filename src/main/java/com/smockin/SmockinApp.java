package com.smockin;

import com.smockin.config.SmockinConfig;
import org.springframework.boot.SpringApplication;

/**
 * Created by mgallina.
 */
public class SmockinApp {

    public static void main(String[] args) {
        SpringApplication.run(SmockinConfig.class, args);
    }

}
