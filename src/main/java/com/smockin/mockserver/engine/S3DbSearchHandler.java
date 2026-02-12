package com.smockin.mockserver.engine;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.admin.persistence.entity.S3MockDir;
import com.smockin.admin.persistence.entity.S3MockFile;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class S3DbSearchHandler {

    @Autowired
    private S3MockDAO s3MockDAO;

    public S3MockFile findS3MockFile(final String expectedPath,
                                     final List<S3MockFile> fromFiles,
                                     final String fromContainer) {

        if (fromFiles.size() == 1) {

            return locateFileForBucket(expectedPath, fromFiles.getFirst(), fromContainer);

        } else {

            for (final S3MockFile s3MockFile : fromFiles) {

                final S3MockFile fromS3MockFile = locateFileForBucket(expectedPath, s3MockFile, fromContainer);

                if (fromS3MockFile != null) {
                    return fromS3MockFile;
                }
            }
        }

        return null;
    }

    public S3MockFile locateFileForBucket(final String expectedPath,
                                          final S3MockFile s3MockFile,
                                          final String container) {

        final S3Mock bucket = s3MockFile.getS3Mock();

        if (bucket != null
                && Strings.CS.equals(container, bucket.getBucketName())) {

            return s3MockFile;
        }

        if (bucket == null) {

            final S3MockDir dir = s3MockFile.getS3MockDir();

            if (dir != null) {

                final StringBuilder filePathTracer = new StringBuilder();
                final S3Mock fromBucket = locateParentBucket(filePathTracer, dir);

                if (fromBucket != null
                        && Strings.CS.equals(container, fromBucket.getBucketName())
                        && Strings.CS.equals(filePathTracer + s3MockFile.getName(), expectedPath)) {

                    return s3MockFile;
                }

            }

        }

        return null;
    }

    public S3MockDir findS3MockDir(final String expectedPath,
                                   final List<S3MockDir> dirs,
                                   final String fromContainer) {

        if (dirs.size() == 1) {

            return locateDirForBucket(expectedPath, dirs.getFirst(), fromContainer);

        } else {

            for (final S3MockDir s3MockDir : dirs) {

                final S3MockDir fromS3MockDir = locateDirForBucket(expectedPath, s3MockDir, fromContainer);

                if (fromS3MockDir != null) {
                    return fromS3MockDir;
                }
            }
        }

        return null;
    }

    public S3MockDir locateDirForBucket(final String expectedPath,
                                        final S3MockDir s3MockDir,
                                        final String container) {

        final S3Mock bucket = s3MockDir.getS3Mock();

        if (bucket != null
                && Strings.CS.equals(container, bucket.getBucketName())) {

            return s3MockDir;
        }

        if (bucket == null) {

            final S3MockDir dir = s3MockDir.getParent();

            if (dir != null) {

                final StringBuilder filePathTracer = new StringBuilder();

                final S3Mock fromBucket = locateParentBucket(filePathTracer, dir);

                if (fromBucket != null
                        && Strings.CS.equals(container, fromBucket.getBucketName())
                        && Strings.CS.equals(filePathTracer + s3MockDir.getName(), sanitiseSeparatorSuffix(expectedPath))) {

                    return s3MockDir;
                }

            }

        }

        return null;
    }

    public String sanitiseSeparatorSuffix(final String value) {

        return ((Strings.CS.endsWith(value, MockedS3ServerEngineUtils.SEPARATOR_CHAR))
                ? Strings.CS.removeEnd(value, MockedS3ServerEngineUtils.SEPARATOR_CHAR)
                : value);
    }

    public S3Mock locateParentBucket(final S3MockDir s3MockDir) {

        return locateParentBucket(null, s3MockDir);
    }

    public S3Mock locateParentBucket(final StringBuilder filePathTracer,
                                     final S3MockDir s3MockDir) {

        if (filePathTracer != null) {
            filePathTracer.insert(0, MockedS3ServerEngineUtils.SEPARATOR_CHAR);
            filePathTracer.insert(0, s3MockDir.getName());
        }

        if (s3MockDir.getS3Mock() != null) {
            return s3MockDir.getS3Mock();
        }

        return locateParentBucket(filePathTracer, s3MockDir.getParent());
    }

    public S3Mock findS3MockByBucketName(final String name) throws RecordNotFoundException {

        final S3Mock s3Mock = s3MockDAO.findByBucketName(name);

        if (s3Mock == null)
            throw new RecordNotFoundException();

        return s3Mock;
    }

}
