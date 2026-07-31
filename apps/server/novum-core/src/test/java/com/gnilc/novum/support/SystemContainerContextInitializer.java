package com.gnilc.novum.support;

import com.gnilc.test.container.FullStackContainerContextInitializer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public final class SystemContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        // A class-level @ContextConfiguration replaces the initializer declared by @ApiTest.
        new FullStackContainerContextInitializer().initialize(context);
        new SystemModuleContextInitializer().initialize(context);
    }
}
