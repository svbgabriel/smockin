package com.smockin.mockserver.engine;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.websocket.LiveLoggingHandler;
import com.smockin.utils.LiveLoggingUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class S3RemoteCallLoggingHandler {

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private LiveLoggingHandler liveLoggingHandler;

    @Autowired
    private S3DbSearchHandler s3DbSearchHandler;

    public void logS3RemoteCall(final String methodName,
                                final Object[] args,
                                final Optional<String> bucketOwnerIdOpt) {

        if (MockedS3ServerEngineUtils.CREATE_CONTAINER_IN_LOCATION_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String) args[1];
            final String bucketOwnerId = bucketOwnerIdOpt.orElseGet(this::loadDefaultUserId);

            handleS3Logging(String.format("Remote client created a new bucket '%s'", containerName), bucketOwnerId);

        } else if (MockedS3ServerEngineUtils.CLEAR_CONTAINER_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String) args[0];

            final String bucketOwnerId = bucketOwnerIdOpt.orElseGet(() -> s3DbSearchHandler.findS3MockByBucketName(containerName).getCreatedBy().getExtId());

            handleS3Logging(String.format("Remote client has cleared all content in bucket '%s'", containerName),
                    bucketOwnerId);

        } else if (MockedS3ServerEngineUtils.DELETE_CONTAINER_METHOD.equalsIgnoreCase(methodName)
                || MockedS3ServerEngineUtils.DELETE_CONTAINER_IF_EMPTY_METHOD.equals(methodName)) {

            final String containerName = (String) args[0];

            final String bucketOwnerId = bucketOwnerIdOpt.orElseGet(() -> s3DbSearchHandler.findS3MockByBucketName(containerName).getCreatedBy().getExtId());

            handleS3Logging(String.format("Remote client has deleted bucket '%s'", containerName),
                    bucketOwnerId);

        } else if (MockedS3ServerEngineUtils.PUT_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String) args[0];
            final org.jclouds.blobstore.domain.Blob blob = (org.jclouds.blobstore.domain.Blob) args[1];
            final String fileName = blob.getMetadata().getName();
            final org.jclouds.io.Payload payload = blob.getPayload();
            final String mimeType = payload.getContentMetadata().getContentType();

            final String bucketOwnerId = bucketOwnerIdOpt.orElseGet(() -> s3DbSearchHandler.findS3MockByBucketName(containerName).getCreatedBy().getExtId());

            if ("application/x-directory".equals(mimeType)) {
                handleS3Logging(String.format("Remote client added directory '%s' to bucket '%s'", fileName, containerName),
                        bucketOwnerId);
                return;
            }

            handleS3Logging(String.format("Remote client added file '%s' to bucket '%s'", fileName, containerName),
                    bucketOwnerId);

        } else if (MockedS3ServerEngineUtils.REMOVE_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String) args[0];
            final String fullFilePathOrDir = (String) args[1];

            final String bucketOwnerId = bucketOwnerIdOpt.orElseGet(() -> s3DbSearchHandler.findS3MockByBucketName(containerName).getCreatedBy().getExtId());

            //
            // Remove Directory
            if (Strings.CS.endsWith(fullFilePathOrDir, MockedS3ServerEngineUtils.SEPARATOR_CHAR)) { // This a dir

                handleS3Logging(String.format("Remote client removed dir '%s' from bucket '%s'", fullFilePathOrDir, containerName),
                        bucketOwnerId);

                return;
            }

            //
            // Remove File
            handleS3Logging(String.format("Remote client removed file '%s' from bucket '%s'", fullFilePathOrDir, containerName),
                    bucketOwnerId);

        } else if (MockedS3ServerEngineUtils.COPY_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            final String fromContainer = (String) args[0];
            final String fromName = (String) args[1];
            final String toContainer = (String) args[2];

            final String bucketOwnerId = bucketOwnerIdOpt.orElseGet(() -> s3DbSearchHandler.findS3MockByBucketName(fromContainer).getCreatedBy().getExtId());

            handleS3Logging(String.format("Remote client copied file '%s' from bucket '%s' into bucket '%s'", fromName, fromContainer, toContainer),
                    bucketOwnerId);

        }

    }

    public String loadDefaultUserId() {

        final Optional<SmockinUser> adminUserOpt = smockinUserService.loadDefaultUser();

        if (adminUserOpt.isEmpty()) {
            throw new RecordNotFoundException();
        }

        return adminUserOpt.get().getExtId();
    }

    public void handleS3Logging(final String message, final String bucketOwnerId) {

        liveLoggingHandler.broadcast(LiveLoggingUtils.buildS3LiveLogging(message, bucketOwnerId));
    }

}
