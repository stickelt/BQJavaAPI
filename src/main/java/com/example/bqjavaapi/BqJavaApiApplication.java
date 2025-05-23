package com.example.bqjavaapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
public class BqJavaApiApplication implements CommandLineRunner {

    @Autowired
    private AspnIdUpdater aspnIdUpdater;

    public static void main(String[] args) {
        SpringApplication.run(BqJavaApiApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        aspnIdUpdater.runBatchJob();
        // Exit after processing - since this will be run as a job
        // System.exit(0);
    }
}
