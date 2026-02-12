package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.admin.persistence.entity.S3MockDir;
import com.smockin.admin.persistence.entity.S3MockFile;
import com.smockin.mockserver.service.S3Client;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

@Component
public class S3BucketInitializer {

    private final Logger logger = LoggerFactory.getLogger(S3BucketInitializer.class);

    @Autowired
    private S3MockDAO s3MockDAO;

    @Autowired
    private S3DbSearchHandler s3DbSearchHandler;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadAndInitBucketContentAsync(final S3Client s3Client,
                                              final String bucketExtId) {
        logger.debug("loadAndInitBucketContentAsync called");

        initBucketContent(s3Client, s3MockDAO.findByExtId(bucketExtId));
    }

    public void initBucketContent(final S3Client s3Client,
                                  final S3Mock bucket) {
        logger.debug("initBucketContent called");

        initBucketContent(s3Client, Collections.singletonList(bucket));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadAndInitBucketContentAsync(final S3Client s3Client,
                                              final List<String> bucketExtIds,
                                              final long userId) {
        logger.debug("loadAndInitBucketContentAsync called");

        initBucketContent(s3Client, s3MockDAO.loadAllActiveByIds(bucketExtIds, userId));
    }

    public void initBucketContent(final S3Client s3Client,
                                  final List<S3Mock> buckets) {
        logger.debug("initBucketContent called");

        // Create all buckets
        buckets.forEach(m -> {

            // If present then remove existing bucket...
            if (s3Client.doesBucketExist(m.getBucketName())) {
                s3Client.deleteBucket(m.getBucketName(), false);
            }

            s3Client.createBucket(m.getBucketName());

        });

        // Create bucket files
        buckets.forEach(m ->
                addBucketFiles(s3Client, m));

        // Start adding bucket dir files
        buckets.forEach(m ->
                commenceAddingBucketDirs(s3Client, m));

    }

    public void addBucketFiles(final S3Client s3Client,
                               final S3Mock bucket) {

        bucket
                .getFiles()
                .forEach(f ->
                        s3Client.uploadObject(
                                bucket.getBucketName(),
                                f.getName(),
                                IOUtils.toInputStream(GeneralUtils.base64Decode(f.getFileContent().getContent()), Charset.defaultCharset()),
                                f.getMimeType()));

    }

    public void commenceAddingBucketDirs(final S3Client s3Client,
                                         final S3Mock bucket) {

        bucket.getChildrenDirs()
                .forEach(d ->
                        addDirFiles(bucket, s3Client, d));

    }

    void addDirFiles(final S3Mock bucket,
                     final S3Client s3Client,
                     final S3MockDir s3MockDir) {

        // Create dir
        final StringBuilder filePathTracer = new StringBuilder();
        s3DbSearchHandler.locateParentBucket(filePathTracer, s3MockDir);
        s3Client.createSubDirectory(bucket.getBucketName(), filePathTracer.toString());

        // Create files in this dir
        s3MockDir
                .getFiles()
                .forEach(f -> {

                    final Pair<String, String> bucketAndFilePath = extractBucketAndFilePath(f);

                    s3Client.uploadObject(
                            bucket.getBucketName(),
                            bucketAndFilePath.getRight(),
                            IOUtils.toInputStream(GeneralUtils.base64Decode(f.getFileContent().getContent()), Charset.defaultCharset()),
                            f.getMimeType());

                });

        // Traverse child dirs
        s3MockDir.getChildren()
                .forEach(d ->
                        addDirFiles(bucket, s3Client, d));

    }

    public Pair<String, String> extractBucketAndFilePath(final S3MockFile s3MockFile) {

        final MutablePair<String, StringBuilder> collectedData
                = new MutablePair<>(null, new StringBuilder(s3MockFile.getName()));

        buildFilePathSegment(s3MockFile.getS3MockDir(), collectedData);

        return new MutablePair<>(collectedData.getLeft(),
                collectedData.getRight().toString());
    }

    void buildFilePathSegment(final S3MockDir s3MockDir,
                              final MutablePair<String, StringBuilder> fileInfo) {

        // Add dir segment
        fileInfo.getRight().insert(0, MockedS3ServerEngineUtils.SEPARATOR_CHAR);
        fileInfo.getRight().insert(0, s3MockDir.getName());

        // Got a bucket so have reached the top of the dir tree
        if (s3MockDir.getS3Mock() != null) {
            fileInfo.setLeft(s3MockDir.getS3Mock().getBucketName());
            return;
        }

        // keep working back up to the tree towards the bucket
        if (s3MockDir.getParent() != null) {
            buildFilePathSegment(s3MockDir.getParent(), fileInfo);
        }

    }

}
