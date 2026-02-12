package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.dao.S3MockDirDAO;
import com.smockin.admin.persistence.dao.S3MockFileDAO;
import com.smockin.admin.persistence.entity.*;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.S3SyncModeEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.io.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class S3RemoteCallPersistenceHandler {

    private final Logger logger = LoggerFactory.getLogger(S3RemoteCallPersistenceHandler.class);

    private static final String APPLICATION_X_DIRECTORY = "application/x-directory";

    @Autowired
    private S3MockDAO s3MockDAO;

    @Autowired
    private S3MockDirDAO s3MockDirDAO;

    @Autowired
    private S3MockFileDAO s3MockFileDAO;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private S3DbSearchHandler s3DbSearchHandler;

    public Optional<String> handlePersistence(final String methodName,
                                              final Object[] args,
                                              final MockedServerConfigDTO configDTO,
                                              final List<String> supportedMethods) {

        if (MockedS3ServerEngineUtils.CREATE_CONTAINER_IN_LOCATION_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String) args[1];

            final Optional<SmockinUser> adminUserOpt = smockinUserService.loadDefaultUser();

            if (adminUserOpt.isEmpty()) {
                return Optional.empty();
            }

            final S3Mock s3Mock = new S3Mock();
            s3Mock.setBucketName(containerName);
            s3Mock.setStatus(RecordStatusEnum.ACTIVE);
            s3Mock.setCreatedBy(adminUserOpt.get());
            s3MockDAO.save(s3Mock);

            return Optional.of(s3Mock.getCreatedBy().getExtId());

        } else if (MockedS3ServerEngineUtils.CLEAR_CONTAINER_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String) args[0];

            final S3Mock s3Mock = s3DbSearchHandler.findS3MockByBucketName(containerName);

            if (!S3SyncModeEnum.BI_DIRECTIONAL.equals(s3Mock.getSyncMode())) {
                return Optional.of(s3Mock.getCreatedBy().getExtId());
            }

            s3Mock.getChildrenDirs().clear();
            s3Mock.getFiles().clear();

            s3MockDAO.save(s3Mock);

            return Optional.of(s3Mock.getCreatedBy().getExtId());

        } else if (MockedS3ServerEngineUtils.DELETE_CONTAINER_METHOD.equalsIgnoreCase(methodName)
                || MockedS3ServerEngineUtils.DELETE_CONTAINER_IF_EMPTY_METHOD.equals(methodName)) {

            final String containerName = (String) args[0];

            final S3Mock s3Mock = s3DbSearchHandler.findS3MockByBucketName(containerName);
            final String createdBy = s3Mock.getCreatedBy().getExtId();

            if (!S3SyncModeEnum.BI_DIRECTIONAL.equals(s3Mock.getSyncMode())) {
                return Optional.of(createdBy);
            }

            s3MockDAO.delete(s3Mock);

            return Optional.of(createdBy);

        } else if (MockedS3ServerEngineUtils.PUT_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String) args[0];
            final Blob blob = (Blob) args[1];
            final String fileName = blob.getMetadata().getName();
            final Payload payload = blob.getPayload();
            final String mimeType = payload.getContentMetadata().getContentType();

            final S3Mock s3Mock = s3DbSearchHandler.findS3MockByBucketName(containerName);
            final String createdBy = s3Mock.getCreatedBy().getExtId();

            if (!S3SyncModeEnum.BI_DIRECTIONAL.equals(s3Mock.getSyncMode())) {
                return Optional.of(createdBy);
            }

            if (APPLICATION_X_DIRECTORY.equals(mimeType)) {
                createS3Dir(fileName, s3Mock);
                return Optional.of(createdBy);
            }

            Optional<String> content;

            try {

                content = MockedS3ServerEngineUtils.buildS3Client(configDTO.getPort()).getObjectContent(containerName, fileName);

                if (content.isEmpty()) {
                    logger.error("Error reading client's uploaded file");
                    return Optional.of(createdBy);
                }

            } catch (Exception ex) {
                logger.error("Error reading client's uploaded file", ex);
                return Optional.of(createdBy);
            }

            createS3DirsAndFile(fileName, mimeType, content.get(), s3Mock);

            return Optional.of(createdBy);

        } else if (MockedS3ServerEngineUtils.REMOVE_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String) args[0];
            final String fullFilePathOrDir = (String) args[1];

            final S3Mock s3Mock = s3DbSearchHandler.findS3MockByBucketName(containerName);
            final String createdBy = s3Mock.getCreatedBy().getExtId();

            if (!S3SyncModeEnum.BI_DIRECTIONAL.equals(s3Mock.getSyncMode())) {
                return Optional.of(createdBy);
            }


            //
            // Remove Directory
            if (Strings.CS.endsWith(fullFilePathOrDir, MockedS3ServerEngineUtils.SEPARATOR_CHAR)) { // This a dir

                final String[] dirsSplitByPath = StringUtils.split(fullFilePathOrDir, MockedS3ServerEngineUtils.SEPARATOR_CHAR);
                final String singleDirectory = (dirsSplitByPath.length > 1)
                        ? dirsSplitByPath[dirsSplitByPath.length - 1]
                        : fullFilePathOrDir;

                final List<S3MockDir> dirs = s3MockDirDAO.findAllByName(Strings.CS.removeEnd(singleDirectory, MockedS3ServerEngineUtils.SEPARATOR_CHAR));

                if (dirs.isEmpty()) {
                    logger.error("Error unable to locate the dir {} in container {} to delete from DB. (No dirs found)", fullFilePathOrDir, containerName);
                    return Optional.of(createdBy);
                }

                final S3MockDir s3MockDir = s3DbSearchHandler.findS3MockDir(fullFilePathOrDir, dirs, containerName);

                if (s3MockDir == null) {
                    logger.error("Error unable to locate the dir {} in container {} to delete from DB. (dir match not made)", fullFilePathOrDir, containerName);
                    return Optional.of(createdBy);
                }

                s3MockDirDAO.delete(s3MockDir);

                return Optional.of(createdBy);
            }

            //
            // Remove File
            final String[] paths = StringUtils.split(fullFilePathOrDir, MockedS3ServerEngineUtils.SEPARATOR_CHAR);
            final String fileName = paths[paths.length - 1];

            final List<S3MockFile> files = s3MockFileDAO.findAllByName(fileName);

            if (files.isEmpty()) {
                logger.error("Error unable to locate the file {} in container {} to delete from DB. (No files found)", fullFilePathOrDir, containerName);
                return Optional.of(createdBy);
            }

            final S3MockFile fromS3MockFile = s3DbSearchHandler.findS3MockFile(fullFilePathOrDir, files, containerName);

            if (fromS3MockFile == null) {
                logger.error("Error unable to locate the file {} in container {} to delete from DB. (file match not made)", fullFilePathOrDir, containerName);
                return Optional.of(createdBy);
            }

            s3MockFileDAO.delete(fromS3MockFile);

            return Optional.of(createdBy);

        } else if (MockedS3ServerEngineUtils.COPY_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            // NOTE, copyBlob seems to just handle the file during a dir rename.

            final String fromContainer = (String) args[0];
            final String fromName = (String) args[1];
            final String toContainer = (String) args[2];
            final String toName = (String) args[3];

            final S3Mock s3Mock = s3DbSearchHandler.findS3MockByBucketName(fromContainer);
            final String createdBy = s3Mock.getCreatedBy().getExtId();

            if (!S3SyncModeEnum.BI_DIRECTIONAL.equals(s3Mock.getSyncMode())) {
                return Optional.of(createdBy);
            }

            final String[] fromPaths = StringUtils.split(fromName, MockedS3ServerEngineUtils.SEPARATOR_CHAR);
            final String fromFileName = fromPaths[fromPaths.length - 1];

            final List<S3MockFile> fromFiles = s3MockFileDAO.findAllByName(fromFileName);

            if (fromFiles.isEmpty()) {
                logger.error("Error unable to locate (from DB) file to copy: {}", fromName);
                return Optional.of(createdBy);
            }

            final S3MockFile fromS3MockFile = s3DbSearchHandler.findS3MockFile(fromName, fromFiles, fromContainer);

            if (fromS3MockFile == null) {
                logger.error("Error unable to locate source file {} from container {} to copy", fromName, fromContainer);
                return Optional.of(createdBy);
            }

            final S3Mock destinationBucket = s3DbSearchHandler.findS3MockByBucketName(toContainer);

            createS3DirsAndFile(toName, fromS3MockFile.getMimeType(), GeneralUtils.base64Decode(fromS3MockFile.getFileContent().getContent()), destinationBucket);

            return Optional.of(createdBy);
        }

        return Optional.empty();
    }

    public void createS3Dir(final String fullDirPath,
                            final S3Mock s3Mock) {

        final String[] paths = StringUtils.split(fullDirPath, MockedS3ServerEngineUtils.SEPARATOR_CHAR);

        // Just the dir to save in the root of the bucket
        if (paths.length == 1) {

            final S3MockDir s3MockDir = new S3MockDir();
            s3MockDir.setName(paths[0]);
            s3MockDir.setS3Mock(s3Mock);
            s3MockDirDAO.save(s3MockDir);
            return;
        }

        // Save dir tree and dir
        handleSubDirsCreation(paths, s3Mock);
    }

    public void createS3DirsAndFile(final String fullFilePath,
                                    final String mimeType,
                                    final String content,
                                    final S3Mock s3Mock) {

        final String[] paths = StringUtils.split(fullFilePath, MockedS3ServerEngineUtils.SEPARATOR_CHAR);

        // Just the file to save in the root of the bucket
        if (paths.length == 1) {
            saveS3MockFile(fullFilePath, mimeType, content, s3Mock, null);
            return;
        }

        // Save dir tree
        final String[] dirPaths = Arrays.copyOf(paths, paths.length - 1);
        S3MockDir parentS3MockDir = handleSubDirsCreation(dirPaths, s3Mock);

        // Save file
        final String fileName = paths[paths.length - 1];
        saveS3MockFile(fileName, mimeType, content, null, parentS3MockDir);

    }

    public S3MockDir handleSubDirsCreation(final String[] paths,
                                           final S3Mock s3Mock) {

        int index = 0;
        S3MockDir parentS3MockDir = null;

        for (String p : paths) {

            // Check if the directory already exists to not duplicate it
            if (index == 0) {

                // check sub dirs in bucket root
                final Optional<S3MockDir> dirOpt = s3Mock
                        .getChildrenDirs()
                        .stream()
                        .filter(d ->
                                d.getName().equals(p))
                        .findFirst();

                if (dirOpt.isPresent()) {
                    parentS3MockDir = dirOpt.get();
                    index++;
                    continue;
                }

            } else {

                // check sub dirs in the current directory
                final Optional<S3MockDir> dirOpt = parentS3MockDir
                        .getChildren()
                        .stream()
                        .filter(d ->
                                d.getName().equals(p))
                        .findFirst();

                if (dirOpt.isPresent()) {
                    parentS3MockDir = dirOpt.get();
                    index++;
                    continue;
                }

            }

            // Create directory
            final S3MockDir s3MockDir = new S3MockDir();
            s3MockDir.setName(p);

            if (index == 0) {
                s3MockDir.setS3Mock(s3Mock);
            }

            s3MockDir.setParent(parentS3MockDir);

            parentS3MockDir = s3MockDirDAO.save(s3MockDir);
            index++;
        }

        return parentS3MockDir;
    }

    public void saveS3MockFile(final String fileName,
                               final String mimeType,
                               final String content,
                               final S3Mock s3Mock,
                               final S3MockDir parentS3MockDir) {

        final S3MockFile s3File = new S3MockFile();
        s3File.setName(fileName);
        s3File.setMimeType(mimeType);
        final S3MockFileContent s3MockFileContent = new S3MockFileContent(s3File, GeneralUtils.base64Encode(content));
        s3File.setFileContent(s3MockFileContent);
        if (s3Mock != null)
            s3File.setS3Mock(s3Mock);
        if (parentS3MockDir != null)
            s3File.setS3MockDir(parentS3MockDir);

        s3MockFileDAO.save(s3File);
    }

}
