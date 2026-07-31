package com.gnilc.novum.support;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootConfiguration
@EnableAutoConfiguration
@AutoConfigurationPackage(basePackages = "com.gnilc.novum")
public class SystemTestApplication {
}
