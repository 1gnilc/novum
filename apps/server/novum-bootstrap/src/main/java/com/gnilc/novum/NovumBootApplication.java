package com.gnilc.novum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.gnilc.novum.inspector")
@MapperScan({
        "com.gnilc.novum.admin.dao",
        "com.gnilc.novum.customer.dao",
        "com.gnilc.novum.image.dao",
        "com.gnilc.novum.i18n.dao"
})
public class NovumBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(NovumBootApplication.class, args);
    }
}
