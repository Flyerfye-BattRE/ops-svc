package com.battre.opssvc;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OpsSvcApplication {

  public static void main(String[] args) {
    SpringApplication.run(OpsSvcApplication.class, args);
  }

  // Needed bc existing DB naming convention is UpperCamelCase while HibernateJPA forces snake_case,
  // causing issues
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
