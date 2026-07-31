package com.gnilc.novum.admin.cache;

import com.gnilc.auth.authz.rbac.event.AuthorizationEvent;
import com.gnilc.auth.authz.rbac.event.MenuEvent;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.novum.admin.event.AdminEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCacheEventListenerTest {
    @Mock
    private AdminCacheService cache;
    @Mock
    private UserRoleService userRoles;

    private AdminCacheEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new AdminCacheEventListener(cache, userRoles);
    }

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void adminAndMenuEventsDeleteTheirOwnedCacheFamilies() {
        listener.handleAdmin(new AdminEvent(AdminEvent.Action.UPDATE, 41L));
        listener.handleMenu(new MenuEvent(MenuEvent.Action.UPDATE, 9L));

        verify(cache).removeUserInfo(41L);
        verify(cache).removeAllMenuAccessCodes();
        verify(cache).removeAllMenuRoutes();
        verify(cache, org.mockito.Mockito.times(2)).scheduleSecondDelete(any());
    }

    @Test
    void authorizationEventsUseTheConfirmedPreciseResetMapping() {
        when(userRoles.getUserIds(7L)).thenReturn(List.of(41L, 42L, 41L));
        when(userRoles.getUserIds(8L)).thenReturn(List.of(43L));

        listener.handleAuthorization(AuthorizationEvent.of(
                AuthorizationEvent.Type.ROLE, AuthorizationEvent.Action.UPDATE, 7L));
        listener.handleAuthorization(AuthorizationEvent.of(
                AuthorizationEvent.Type.ROLE_MENU, AuthorizationEvent.Action.REPLACE, 8L));
        listener.handleAuthorization(AuthorizationEvent.of(
                AuthorizationEvent.Type.USER_ROLE, AuthorizationEvent.Action.REPLACE, 44L));

        verify(cache).removeRoleCodes(41L);
        verify(cache).removeRoleCodes(42L);
        verify(cache).removeMenuAccessCodes(43L);
        verify(cache).removeMenuRoutes(43L);
        verify(cache).removeRoleCodes(44L);
        verify(cache).removeMenuAccessCodes(44L);
        verify(cache).removeMenuRoutes(44L);
    }

    @Test
    void permissionEventsDoNotDeleteAdminQueryCaches() {
        listener.handleAuthorization(AuthorizationEvent.of(
                AuthorizationEvent.Type.PERMISSION, AuthorizationEvent.Action.UPDATE, 7L));
        listener.handleAuthorization(AuthorizationEvent.of(
                AuthorizationEvent.Type.ROLE_PERMISSION, AuthorizationEvent.Action.REPLACE, 8L));

        verifyNoInteractions(cache, userRoles);
    }

    @Test
    void allAuthorizationEventDeletesAllRoleAndMenuQueries() {
        listener.handleAllAuthorization(AuthorizationEvent.of(
                AuthorizationEvent.Type.ALL, AuthorizationEvent.Action.CLEAR));

        verify(cache).removeAllRoleCodes();
        verify(cache).removeAllMenuAccessCodes();
        verify(cache).removeAllMenuRoutes();
        verify(cache).scheduleSecondDelete(any());
    }

    @Test
    void delayedDeleteIsRegisteredOnlyForAfterCommitWhenTransactionIsActive() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        listener.handleAdmin(new AdminEvent(AdminEvent.Action.UPDATE, 45L));

        verify(cache).removeUserInfo(45L);
        verify(cache, never()).scheduleSecondDelete(any());
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        clearInvocations(cache);

        synchronizations.forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<Runnable> delayed = ArgumentCaptor.forClass(Runnable.class);
        verify(cache).scheduleSecondDelete(delayed.capture());
        delayed.getValue().run();
        verify(cache).removeUserInfo(45L);
    }
}
