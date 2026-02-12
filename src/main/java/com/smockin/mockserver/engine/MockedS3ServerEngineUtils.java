package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.dao.S3MockDirDAO;
import com.smockin.admin.persistence.dao.S3MockFileDAO;
import com.smockin.admin.persistence.entity.*;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.websocket.LiveLoggingHandler;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.service.S3Client;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
public class MockedS3ServerEngineUtils {

    private final Logger logger = LoggerFactory.getLogger(MockedS3ServerEngineUtils.class);

    private List<String> supportedInternalS3ClientUpdateMethods; // i.e BlobStore update based methods based on the functions present in S3Client

    // Using hardcoded forward slash rather then File.separatorChar as not sure how this will behave on Windows machines
    public static final String SEPARATOR_CHAR = GeneralUtils.URL_PATH_SEPARATOR;

    public static final String CREATE_CONTAINER_IN_LOCATION_METHOD = "createContainerInLocation";
    public static final String DELETE_CONTAINER_METHOD = "deleteContainer";
    public static final String CLEAR_CONTAINER_METHOD = "clearContainer";
    public static final String PUT_BLOB_METHOD = "putBlob";
    public static final String REMOVE_BLOB_METHOD = "removeBlob";
    public static final String COPY_BLOB_METHOD = "copyBlob";
    public static final String CONTAINER_EXISTS_METHOD = "containerExists";
    public static final String DELETE_CONTAINER_IF_EMPTY_METHOD = "deleteContainerIfEmpty";
    public static final String REMOVE_BLOBS_METHOD = "removeBlobs";
    public static final String LIST_METHOD = "list";
    public static final String BLOB_BUILDER_METHOD = "blobBuilder";
    public static final String GET_BLOB_METHOD = "getBlob";
    public static final String DOES_BUCKET_EXIST_METHOD = "containerExists";


    @Autowired
    private S3MockDAO s3MockDAO;

    @Autowired
    private S3MockDirDAO s3MockDirDAO;

    @Autowired
    private S3MockFileDAO s3MockFileDAO;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private LiveLoggingHandler liveLoggingHandler;

    @Autowired
    private S3DbSearchHandler s3DbSearchHandler;

    @Autowired
    private S3BucketInitializer s3BucketInitializer;

    @Autowired
    private S3RemoteCallPersistenceHandler s3RemoteCallPersistenceHandler;

    @Autowired
    private S3RemoteCallLoggingHandler s3RemoteCallLoggingHandler;


    public Optional<String> persistS3RemoteCall(final String methodName,
                                    final Object[] args,
                                    final MockedServerConfigDTO configDTO) {

        logger.debug("persistS3RemoteCall called");

        if (!supportedInternalS3ClientUpdateMethods.contains(methodName)) {
            return Optional.empty();
        }

        return s3RemoteCallPersistenceHandler.handlePersistence(methodName, args, configDTO, supportedInternalS3ClientUpdateMethods);
    }

    public void logS3RemoteCall(final String methodName,
                                final Object[] args,
                                final Optional<String> bucketOwnerIdOpt) {

        logger.debug("logS3RemoteCall called");

        if (!supportedInternalS3ClientUpdateMethods.contains(methodName)) {
            return;
        }

        s3RemoteCallLoggingHandler.logS3RemoteCall(methodName, args, bucketOwnerIdOpt);
    }

    @Transactional(readOnly = true)
    public S3MockFile findS3MockFile(final String expectedPath,
                                      final List<S3MockFile> fromFiles,
                                      final String fromContainer) {
        return s3DbSearchHandler.findS3MockFile(expectedPath, fromFiles, fromContainer);
    }

    @Transactional(readOnly = true)
    public S3MockDir findS3MockDir(final String expectedPath,
                            final List<S3MockDir> dirs,
                            final String fromContainer) {
        return s3DbSearchHandler.findS3MockDir(expectedPath, dirs, fromContainer);
    }

    public void handleS3Logging(final String message, final String bucketOwnerId) {
        s3RemoteCallLoggingHandler.handleS3Logging(message, bucketOwnerId);
    }

    public Object[] sanitiseContainerNameInArgs(final Object[] originalArgs,
                                                final String methodName) {

        logger.debug("sanitiseContainerNameInArgs called");

        if (!supportedInternalS3ClientUpdateMethods.contains(methodName)) {
            return originalArgs;
        }


        if (originalArgs == null || originalArgs.length == 0) {
            return originalArgs;
        }

        return IntStream
                    .range(0, originalArgs.length)
                    .mapToObj(index -> {

                        final Object arg = originalArgs[index];

                        if (arg instanceof String
                                && StringUtils.startsWith((String)arg, S3Client.SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX)) {

                            return StringUtils.removeStart((String)arg, S3Client.SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX);
                        }

                        return arg;
                    })
                    .collect(Collectors.toList())
                    .toArray();
    }

    public Optional<Boolean> isCallInternal(final Object[] originalArgs,
                                            final String methodName) {
        logger.debug("isCallInternal called");

        if (!supportedInternalS3ClientUpdateMethods.contains(methodName)) {
            return Optional.empty();
        }

        if (originalArgs == null || originalArgs.length == 0) {
            return Optional.empty();
        }

        final boolean matchMade = IntStream
            .range(0, originalArgs.length)
            .anyMatch(i -> {
                final Object arg = originalArgs[i];
                return (arg instanceof String)
                            && StringUtils.startsWith((String)arg, S3Client.SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX);
            });

        return Optional.of(matchMade);
    }

    @Transactional(readOnly = true)
    public S3Mock locateParentBucket(final S3MockDir s3MockDir) {
        return s3DbSearchHandler.locateParentBucket(s3MockDir);
    }

    @Transactional(readOnly = true)
    public S3Mock locateParentBucket(final StringBuilder filePathTracer,
                                     final S3MockDir s3MockDir) {
        return s3DbSearchHandler.locateParentBucket(filePathTracer, s3MockDir);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadAndInitBucketContentAsync(final S3Client s3Client,
                                              final String bucketExtId) {
        s3BucketInitializer.loadAndInitBucketContentAsync(s3Client, bucketExtId);
    }

    public void initBucketContent(final S3Client s3Client,
                                  final S3Mock bucket) {
        s3BucketInitializer.initBucketContent(s3Client, bucket);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadAndInitBucketContentAsync(final S3Client s3Client,
                                              final List<String> bucketExtIds,
                                              final long userId) {
        s3BucketInitializer.loadAndInitBucketContentAsync(s3Client, bucketExtIds, userId);
    }

    public void initBucketContent(final S3Client s3Client,
                                  final List<S3Mock> buckets) {
        s3BucketInitializer.initBucketContent(s3Client, buckets);
    }

    @Transactional(readOnly = true)
    public Pair<String, String> extractBucketAndFilePath(final S3MockFile s3MockFile) {
        return s3BucketInitializer.extractBucketAndFilePath(s3MockFile);
    }

    public static S3Client buildS3Client(final int port) {

        return new S3Client(
                GeneralUtils.S3_HOST,
                port);
    }

    @PostConstruct
    public void after() {

        supportedInternalS3ClientUpdateMethods = Arrays.asList(
            CREATE_CONTAINER_IN_LOCATION_METHOD
            , DELETE_CONTAINER_METHOD
            , CLEAR_CONTAINER_METHOD
            , PUT_BLOB_METHOD
            , REMOVE_BLOB_METHOD
            , COPY_BLOB_METHOD
            , CONTAINER_EXISTS_METHOD
            , DELETE_CONTAINER_IF_EMPTY_METHOD
            , REMOVE_BLOBS_METHOD
            , LIST_METHOD
            , BLOB_BUILDER_METHOD
            , GET_BLOB_METHOD
            , DOES_BUCKET_EXIST_METHOD
        );

    }

}
