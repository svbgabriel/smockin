package com.smockin.admin.service;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.UserKeyValueDataDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.entity.UserKeyValueData;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserKeyValueDataServiceTest {

    @Mock
    private UserKeyValueDataDAO userKeyValueDataDAO;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @InjectMocks
    private UserKeyValueDataServiceImpl userKeyValueDataService;

    @Test
    void loadAll_returnsDtos() throws RecordNotFoundException {

        // Setup
        final SmockinUser user = new SmockinUser();
        user.setId(9L);

        final UserKeyValueData data = new UserKeyValueData();
        data.setExtId("kv-1");
        data.setKey("foo");
        data.setValue("bar");

        Mockito.when(userTokenServiceUtils.loadCurrentActiveUser("token")).thenReturn(user);
        Mockito.when(userKeyValueDataDAO.findAllByUser(9L)).thenReturn(List.of(data));

        // Test
        final List<UserKeyValueDataDTO> result = userKeyValueDataService.loadAll("token");

        // Assertions
        Assertions.assertEquals(1, result.size());
        final UserKeyValueDataDTO dto = result.getFirst();
        Assertions.assertEquals("kv-1", dto.getExtId());
        Assertions.assertEquals("foo", dto.getKey());
        Assertions.assertEquals("bar", dto.getValue());
    }
}
