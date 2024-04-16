package com.battre.opssvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@SpringBootApplication
public class OpssvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpssvcApplication.class, args);
    }

    // Needed bc existing DB naming convention is UpperCamelCase while HibernateJPA forces snake_case, causing issues
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return new HibernatePropertiesCustomizer() {
            @Override
            public void customize(Map<String, Object> hibernateProperties) {
                hibernateProperties.put("hibernate.physical_naming_strategy", CustomNamingStrategy.class);
            }
        };
    }

}
