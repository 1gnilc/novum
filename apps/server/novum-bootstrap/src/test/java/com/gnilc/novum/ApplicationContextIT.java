package com.gnilc.novum;

import com.gnilc.auth.authn.servlet.filter.ServletAuthenticationFilter;
import com.gnilc.auth.authz.decision.AccessDecision;
import com.gnilc.auth.authz.servlet.filter.ServletAuthorizationFilter;
import com.gnilc.novum.admin.service.AdminService;
import com.gnilc.novum.image.service.ImageService;
import com.gnilc.novum.image.support.ObjectKeyToUrlProcessor;
import com.gnilc.novum.support.BootstrapContainerContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = NovumBootApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = BootstrapContainerContextInitializer.class)
@TestPropertySource(properties = {
        "app.s3.endpoint=http://127.0.0.1:9",
        "app.s3.region=auto",
        "app.s3.bucket=test-images",
        "app.s3.access-key=test-access-key",
        "app.s3.secret-key=test-secret-key",
        "app.s3.public-base-url=https://images.example.test",
        "app.s3.cleanup-cron=-"
})
class ApplicationContextIT {
    @Autowired
    private ApplicationContext context;

    @Test
    void productionAutoConfigurationsComposeTheCompleteApplication() {
        assertThat(context.getBean(AdminService.class)).isNotNull();
        assertThat(context.getBean(ImageService.class)).isNotNull();
        assertThat(context.getBean(ObjectKeyToUrlProcessor.class)).isNotNull();
        assertThat(context.getBean(AccessDecision.class)).isNotNull();
        assertThat(context.getBean(ServletAuthenticationFilter.class)).isNotNull();
        assertThat(context.getBean(ServletAuthorizationFilter.class)).isNotNull();
        assertThat(context.getBean(StringRedisTemplate.class)).isNotNull();
    }
}
