package com.gnilc.novum.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionManagerTest {

    @Test
    void domainSessionManagersImplementTheCommonContract() {
        assertThat(AdminSessionManager.class).isAssignableTo(SessionManager.class);
        assertThat(CustomerSessionManager.class).isAssignableTo(SessionManager.class);
    }
}
