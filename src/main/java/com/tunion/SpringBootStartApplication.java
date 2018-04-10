package com.tunion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by Think on 2017/11/16.
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude={JpaRepositoriesAutoConfiguration.class})
@ComponentScan(basePackages = { "com.tunion" })
public class SpringBootStartApplication {

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(SpringBootStartApplication.class);
        app.run(args);
    }
}
