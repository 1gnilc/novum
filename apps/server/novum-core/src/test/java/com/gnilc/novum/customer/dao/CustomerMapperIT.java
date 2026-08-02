package com.gnilc.novum.customer.dao;

import com.gnilc.novum.customer.entity.bo.CustomerBo;
import com.gnilc.novum.support.SystemContainerContextInitializer;
import com.gnilc.novum.support.SystemTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
@Transactional
class CustomerMapperIT {
    @Autowired private CustomerDao customers;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void customerMappingUsesUniqueIndexesAutoFillAndLogicalDelete() {
        CustomerBo customer = customer("mapper-customer", 9001L);
        customers.insert(customer);

        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getCreateTime()).isNotNull();
        assertThat(customers.selectById(customer.getId()).getNickname()).isEqualTo("mapper-customer");
        assertThat(customers.selectById(Long.MAX_VALUE)).isNull();
        assertThatThrownBy(() -> customers.insert(customer("mapper-customer", 9002L)))
                .isInstanceOf(DuplicateKeyException.class);

        customers.deleteById(customer.getId());

        assertThat(customers.selectById(customer.getId())).isNull();
        assertThat(jdbc.queryForObject(
                "select del from nv_customer where id = ?", Integer.class, customer.getId())).isEqualTo(1);
    }

    private CustomerBo customer(String username, Long userId) {
        CustomerBo customer = new CustomerBo();
        customer.setUserId(userId);
        customer.setUsername(username);
        customer.setPassword("hash");
        customer.setNickname(username);
        customer.setStatus(true);
        return customer;
    }
}
