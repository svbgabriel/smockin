package com.smockin.admin.service;

import com.smockin.admin.dto.S3MockBucketDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.dao.S3MockDirDAO;
import com.smockin.admin.persistence.dao.S3MockFileDAO;
import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.S3SyncModeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.engine.MockedS3ServerEngineUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3MockServiceTest {

    @Mock
    private S3MockDAO s3MockDAO;

    @Mock
    private S3MockDirDAO s3MockDirDAO;

    @Mock
    private S3MockFileDAO s3MockFileDAO;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private MockedServerEngineService mockedServerEngineService;

    @Mock
    private MockedS3ServerEngineUtils mockedS3ServerEngineUtils;

    @InjectMocks
    private S3MockServiceImpl s3MockService;

    @Test
    void createS3Bucket_returnsExternalId() throws RecordNotFoundException, ValidationException {

        // Setup
        final S3MockBucketDTO dto = new S3MockBucketDTO("bucket-1", RecordStatusEnum.INACTIVE, S3SyncModeEnum.NO_SYNC);
        final SmockinUser user = new SmockinUser();
        user.setId(1L);

        final S3Mock saved = new S3Mock();
        saved.setExtId("bucket-ext-1");

        Mockito.when(userTokenServiceUtils.loadCurrentActiveUser("token")).thenReturn(user);
        Mockito.when(s3MockDAO.save(Mockito.any(S3Mock.class))).thenReturn(saved);

        // Test
        final String result = s3MockService.createS3Bucket(dto, "token");

        // Assertions
        Assertions.assertEquals("bucket-ext-1", result);
    }
}
