package com.smockin.admin.service.utils;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.SmockinUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserTokenServiceUtilsTest {

    @Mock
    private SmockinUserService smockinUserService;

    @InjectMocks
    private UserTokenServiceUtils userTokenServiceUtils;

    @Test
    void loadCurrentActiveUser_inactiveMode_usesDefaultUser() throws RecordNotFoundException {

        // Setup
        final SmockinUser defaultUser = new SmockinUser();
        defaultUser.setId(1L);

        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.INACTIVE);
        Mockito.when(smockinUserService.loadDefaultUser()).thenReturn(Optional.of(defaultUser));

        // Test
        final SmockinUser result = userTokenServiceUtils.loadCurrentActiveUser("token");

        // Assertions
        Assertions.assertEquals(1L, result.getId());
    }
}
