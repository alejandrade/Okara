package io.shrouded.okara;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMongock
public class OkaraApplication {

    public static void main(String[] args) {
        SpringApplication.run(OkaraApplication.class, args);
    }
}