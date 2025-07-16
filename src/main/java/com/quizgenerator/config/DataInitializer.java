package com.quizgenerator.config;

import com.quizgenerator.service.StandardsService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class DataInitializer implements CommandLineRunner {

    private StandardsService standardsService;
    
    @Override
    public void run(String... args) {
        standardsService.initializeStandards();
    }
}