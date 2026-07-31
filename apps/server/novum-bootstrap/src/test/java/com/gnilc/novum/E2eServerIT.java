package com.gnilc.novum;

import com.gnilc.novum.support.BootstrapContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.CountDownLatch;

/** Keeps the complete application alive while Playwright exercises it. */
@SpringBootTest(
        classes = NovumBootApplication.class,
        properties = "server.port=3766",
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = BootstrapContainerContextInitializer.class)
@EnabledIfEnvironmentVariable(named = "RUN_E2E_SERVER", matches = "true")
class E2eServerIT {
    @Test
    void serveUntilThePlaywrightProcessStops() throws InterruptedException {
        new CountDownLatch(1).await();
    }
}
