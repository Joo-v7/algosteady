package com.algosteady;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Algosteady 백엔드 시작점 */
@SpringBootApplication
public class BackendApplication {

    /**
     * Spring Boot 서버 시작
     * @param args 애플리케이션 실행 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
