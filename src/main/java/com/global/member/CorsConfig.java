package com.global.member;


	
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

	  @Bean
	  public WebMvcConfigurer corsConfigurer() {
	       return new WebMvcConfigurer() {
	           @Override
	           public void addCorsMappings(CorsRegistry registry) {
	           registry.addMapping("/email/**") // 이메일 관련 API 허용
	                   .allowedOrigins("*") // 모든 도메인 허용 (보안 설정 필요하면 수정)
	                   .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
	                   .allowCredentials(true);
	            }
	        };
	    }
	}

