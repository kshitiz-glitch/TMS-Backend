package com.kshitizgaur.tms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Transport Management System API", version = "1.0.0", description = "Backend API for Transport Management System (TMS) - Handles load posting, transporter bidding, and booking management", contact = @Contact(name = "Kshitiz Gaur", email = "careers@cargopro.ai")))
public class TmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmsApplication.class, args);
    }
}
