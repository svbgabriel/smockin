package com.smockin.admin.controller;

import com.smockin.admin.dto.S3MockBucketDTO;
import com.smockin.admin.dto.S3MockDirDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseLiteDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.enums.S3MockTypeEnum;
import com.smockin.admin.exception.FileUploadException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.S3MockService;
import com.smockin.utils.GeneralUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Created by mgallina.
 */
@RestController
public class S3MockController {


    private final S3MockService s3MockService;

    public S3MockController(S3MockService s3MockService) {
        this.s3MockService = s3MockService;
    }


    @PostMapping(path="/s3mock/bucket", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleMessageResponseDTO<String>> createBucket(@RequestBody final S3MockBucketDTO dto,
                                                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(
                s3MockService.createS3Bucket(dto,
                        GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @PostMapping(path="/s3mock/dir", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleMessageResponseDTO<String>> createDir(@RequestBody final S3MockDirDTO dto,
                                                                                    @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(
                s3MockService.createS3BucketDir(dto,
                    GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @PostMapping(path="/s3mock/bucket/{extId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SimpleMessageResponseDTO<String>> uploadFileToBucket(@PathVariable final String extId,
                                                                               @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken,
                                                                               @RequestParam("file") final MultipartFile file)
                                                                                    throws RecordNotFoundException, ValidationException, FileUploadException {

        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(
                s3MockService.uploadS3BucketFile(extId, S3MockTypeEnum.BUCKET, file,
                    GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @PostMapping(path="/s3mock/bucket/{extId}/resynchronize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resynchronizeS3Bucket(@PathVariable final String extId,
                                                      @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        s3MockService.resetS3BucketOnMockServer(extId, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping(path="/s3mock/dir/{extId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SimpleMessageResponseDTO<String>> uploadFileToDir(@PathVariable final String extId,
                                                                            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken,
                                                                            @RequestParam("file") final MultipartFile file)
            throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(
                s3MockService.uploadS3BucketFile(extId, S3MockTypeEnum.DIR, file,
                        GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @PutMapping(path = "/s3mock/bucket/{extId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateBucket(@PathVariable final String extId,
                                                             @RequestBody final S3MockBucketDTO dto,
                                                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        s3MockService.updateS3Bucket(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping(path = "/s3mock/dir/{extId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateDir(@PathVariable final String extId,
                                            @RequestBody final S3MockDirDTO dto,
                                            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        s3MockService.updateS3Dir(extId, dto,
                GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(path = "/s3mock/bucket/{extId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteBucket(@PathVariable final String extId,
                                                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {

        s3MockService.deleteS3BucketOrFile(extId, S3MockTypeEnum.BUCKET,
                GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(path = "/s3mock/dir/{extId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteDir(@PathVariable final String extId,
                                                          @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        s3MockService.deleteS3BucketOrFile(extId, S3MockTypeEnum.DIR,
                GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(path = "/s3mock/file/{extId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteFile(@PathVariable final String extId,
                                                           @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        s3MockService.deleteS3BucketOrFile(extId, S3MockTypeEnum.FILE, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path="/s3mock/bucket", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<S3MockBucketResponseLiteDTO>> getAllBuckets(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                throws RecordNotFoundException {

        return new ResponseEntity<>(
                s3MockService.loadAll(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @GetMapping(path="/s3mock/bucket/{extId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<S3MockBucketResponseDTO> getBucket(@PathVariable final String extId,
                                                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws ValidationException, RecordNotFoundException {

        return new ResponseEntity<>(
                s3MockService.loadById(extId,
                        GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

}

