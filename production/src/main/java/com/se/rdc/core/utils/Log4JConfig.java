package com.se.rdc.core.utils;

import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Log4JConfig {
    PropertyConfigurator configurator = new PropertyConfigurator();
    Log4JConfig(){
        PropertyConfigurator.configure("D:\\SEBPO\\list-importer-se-11\\assets\\config\\log4j.properties");
    }
}
