package com.algosteady;

import com.algosteady.judging.service.ExecutionWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 백엔드 애플리케이션 시작 검증 */
@SpringBootTest
class BackendApplicationTests {

    @MockitoBean
    private ExecutionWorker worker;

    @Test
    @DisplayName("백엔드 애플리케이션을 시작합니다.")
    void contextLoads() {
    }

}
