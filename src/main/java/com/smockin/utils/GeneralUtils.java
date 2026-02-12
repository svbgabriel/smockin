package com.smockin.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.smockin.admin.enums.UserModeEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.AntPathMatcher;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by mgallina.
 */
public final class GeneralUtils {

    private GeneralUtils() {}

    private static final Logger logger = LoggerFactory.getLogger(GeneralUtils.class);

    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String UNIQUE_TIMESTAMP_FORMAT = "yyMMdd-HHmmss";

    public static final String OAUTH_HEADER_VALUE_PREFIX = "Bearer";
    public static final String OAUTH_HEADER_NAME = "Authorization";
    public static final String KEEP_EXISTING_HEADER_NAME = "KeepExisting";

    public static final String ENABLE_CORS_PARAM = "ENABLE_CORS";
    public static final String AUTO_GEN_INBOXES_PARAM = "AUTO_GEN_INBOXES";
    public static final String NGROK_AUTH_TOKEN = "NGROK_AUTH_TOKEN";

    public static final String S3_HOST = "localhost";

    public static final String LOG_REQ_ID = "X-Smockin-Trace-ID";
    public static final String PROXIED_DOWNSTREAM_URL_HEADER = "X-Proxied-Downstream-Url";
    public static final String PATH_WILDCARD = "*";
    public static final String URL_PATH_SEPARATOR = "/";
    public static final String CARRIAGE = "\n";

    public static final int DEFAULT_RECORDS_PER_PAGE = 25;

    static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    static {
        JSON_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JSON_MAPPER.registerModule(new Jdk8Module());
    }


    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    // Should be set to UTC from the command line
    public static Date getCurrentDate() {
        return Date.from(getCurrentDateTime().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date toDate(final LocalDateTime localDateTime) {
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    public static Instant getCurrentDateTimeInstant() {
        return getCurrentDateTime().toInstant(ZoneOffset.UTC);
    }

    public static String createFileNameUniqueTimeStamp() {
        return new SimpleDateFormat(UNIQUE_TIMESTAMP_FORMAT)
                .format(getCurrentDate());
    }

    /**
     *
     * Returns the header value for the given name.
     * Lookup is case-insensitive
     *
     * @param request
     * @param headerName
     * @returns String
     *
     */
    public static String findHeaderIgnoreCase(final HttpServletRequest request, final String headerName) {

        for (Iterator<String> it = request.getHeaderNames().asIterator(); it.hasNext(); ) {
            String h = it.next();
            if (h.equalsIgnoreCase(headerName)) {
                return request.getHeader(h);
            }
        }

        return null;
    }

    public static String findPathVarIgnoreCase(final String inboundPath, final String mockPath, final String pathVarName) {

        if (pathVarName == null) {
            return null;
        }

        return findAllPathVars(inboundPath, mockPath).get(pathVarName.toLowerCase());
    }

    public static Map<String, String> findAllPathVars(final String inboundPath, final String mockPath) {

        final String[] inboundPathSegments = StringUtils.split(inboundPath, URL_PATH_SEPARATOR);
        final String[] mockPathSegments = StringUtils.split(mockPath, URL_PATH_SEPARATOR);

        final Map<String, String> pathVars = new HashMap<>();

        if (inboundPathSegments.length < mockPathSegments.length) {
            return pathVars;
        }

        int index = 0;

        for (String segment : mockPathSegments) {

            if (segment != null
                    && ("*".equals(segment)
                            || (segment.startsWith("{") && segment.endsWith("}")))) {

                segment = Strings.CS.remove(segment, "{");
                segment = Strings.CS.remove(segment, "}");

                if ("*".equals(segment)) {
                    segment = "*" + index;
                }

                pathVars.put(segment.toLowerCase(), inboundPathSegments[index]);
            }

            index++;
        }

        return pathVars;
    }

    public static String sanitizeMultiUserPath(final UserModeEnum usermode, final String pathInfo, final String ctxPath) {

        return ( UserModeEnum.ACTIVE.equals(usermode) && StringUtils.isNotBlank(ctxPath) )
                    ? RegExUtils.removeFirst(pathInfo, ctxPath)
                    : pathInfo;
    }

    public static void checkForAndHandleSleep(final long sleepInMillis) {

        if (sleepInMillis > 0) {
            try {
                Thread.sleep(sleepInMillis);
            } catch (InterruptedException ex) {
                logger.error("Error pausing response for the specified period of {}", sleepInMillis, ex);
            }
        }
    }

    public static String prefixPath(final String path) {

        if (StringUtils.isBlank(path)) {
            return null;
        }

        if (!path.startsWith(URL_PATH_SEPARATOR)) {
            return URL_PATH_SEPARATOR + path;
        }

        return path;
    }

    public static int exactVersionNo(String versionNo) {

        if (versionNo == null)
            throw new IllegalArgumentException("versionNo is not defined");

        versionNo = Strings.CI.remove(versionNo, "-SNAPSHOT");
        versionNo = Strings.CS.remove(versionNo, ".");

        if (!NumberUtils.isDigits(versionNo))
            throw new IllegalArgumentException("extracted versionNo is not a valid number: " + versionNo);

        return Integer.parseInt(versionNo);
    }

    public static String removeAllLineBreaks(final String original) {
        return RegExUtils.replaceAll(original, System.lineSeparator(), "");
    }

    public static List<Map<String, ?>> deserialiseJSONToList(final String jsonStr) {
        return deserializeJson(jsonStr, false);
    }

    public static Map<String, ?> deserialiseJSONToMap(final String jsonStr) {
        return deserializeJson(jsonStr, false);
    }

    public static <T> T deserializeJson(final String jsonStr) {
        return deserializeJson(jsonStr, true);
    }

    public static Map<String, ?> deserialiseJSONToMap(final String jsonStr, final boolean logFailure) {
        return deserializeJson(jsonStr, logFailure);
    }

    public static <T> T deserializeJson(final String jsonStr, final boolean logFailure) {

        if (jsonStr != null) {
            try {
                return JSON_MAPPER.readValue(jsonStr, new TypeReference<T>() {});
            } catch (IOException e) {
                if (logFailure) {
                    logger.error("Error de-serialising json", e);
                }
                // always fail silently
            }
        }

        return null;
    }

    public static <T> T deserializeJson(final String jsonStr, final TypeReference<T> type) {

        if (jsonStr != null) {
            try {
                return JSON_MAPPER.readValue(jsonStr, type);
            } catch (IOException e) {
                logger.error("Error de-serialising json", e);
                // fail silently
            }
        }

        return null;
    }

    public static <T> String serialiseJson(final T t) {

        try {
            return JSON_MAPPER.writeValueAsString(t);
        } catch (JsonProcessingException e) {
            logger.error("Error serialising json", e);
            // fail silently
        }

        return null;
    }

    public static String extractOAuthToken(final String bearerToken) {

        if (bearerToken == null) {
            return null;
        }

        return Strings.CS.replace(bearerToken, OAUTH_HEADER_VALUE_PREFIX, "").trim();
    }

    public static String getFileTypeExtension(final String fileName) {

        if (fileName == null) {
            return null;
        }

        final int extPos = fileName.lastIndexOf(".");

        if (extPos == -1) {
            return null;
        }

        return fileName.substring(extPos);
    }

    public static byte[] createArchive(final String zipFileName, final byte[] zipFileContent) {

        ZipOutputStream zos = null;
        ByteArrayOutputStream baos = null;

        try {

            baos = new ByteArrayOutputStream();
            zos = new ZipOutputStream(baos);
            final ZipEntry entry = new ZipEntry(zipFileName);

            entry.setSize(zipFileContent.length);

            zos.putNextEntry(entry);

            zos.write(zipFileContent);
            zos.closeEntry();

            closeOSQuietly(zos);

            return baos.toByteArray();

        } catch (Exception ex) {
            logger.error("Error creating zip archive", ex);

            closeOSQuietly(zos);
            closeOSQuietly(baos);
        }

        return null;
    }

    public static void unpackArchive(final String zipFilePath, final String destDir) {

        final File dir = new File(destDir);

        if (!dir.exists())
            dir.mkdirs();

        final byte[] buffer = new byte[1024];
        FileInputStream fis = null;

        try {

            fis = new FileInputStream(zipFilePath);
            final ZipInputStream zis = new ZipInputStream(fis);

            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {

                final File newFile = new File(destDir, ze.getName());

                if (!newFile.toPath().normalize().startsWith(destDir)) {
                    throw new RuntimeException("Bad zip entry");
                }

                if (ze.isDirectory()) {

                    newFile.mkdir();

                } else {

                    FileOutputStream fos = null;

                    try {

                        fos = new FileOutputStream(newFile);
                        int len;

                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }

                    } finally {
                        closeOSQuietly(fos);
                    }

                }

                zis.closeEntry();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            closeISQuietly(zis);

        } catch (IOException e) {
            logger.error("Error unpacking archive file", e);
        } finally {
            closeISQuietly(fis);
        }

    }

    private static void closeISQuietly(final InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                logger.error("Error closing input stream", e);
            }
        }
    }

    private static void closeOSQuietly(final OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                logger.error("Error closing output stream", e);
            }
        }
    }

    public static String extractRequestParamByName(final HttpServletRequest req, final String fieldName) {

        return extractAllRequestParams(req)
                .entrySet()
                .stream()
                .filter(p ->
                        Strings.CI.equals(fieldName, p.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static Map<String, String> extractAllRequestParams(final HttpServletRequest req) {

        final Map<String, String> allParams = new HashMap<>();

        final Map<String, String[]> parameterMap = req.getParameterMap();

        parameterMap.forEach((key, values) -> {
            if (values != null && values.length > 0) {
                allParams.put(key, values[0]);
            } else {
                allParams.put(key, null);
            }
        });

        return allParams;
    }

    public static String removeJsComments(final String jsSrc) {

        final String comment = "//";

        if (jsSrc == null
                || Strings.CS.indexOf(jsSrc, comment) == -1) {
            return jsSrc;
        }

        // i.e., single line
        if (Strings.CS.indexOf(jsSrc, CARRIAGE) == -1) {
            return StringUtils.substring(jsSrc, 0, Strings.CS.indexOf(jsSrc, comment)).trim();
        }

        final String[] lines = StringUtils.split(jsSrc, CARRIAGE);

        return Stream.of(lines)
                .filter(l ->
                    !l.trim().startsWith(comment))
                .map(l -> {

                    final int commentInLine = Strings.CS.indexOf(l, comment);

                    return (commentInLine > -1)
                            ? StringUtils.substring(l, 0, commentInLine)
                            : l;
                })
                .collect(Collectors.joining(CARRIAGE))
                .trim();
    }

    public static boolean matchPaths(final String mockPath, final String inboundPath) {

        final AntPathMatcher matcher = new AntPathMatcher(AntPathMatcher.DEFAULT_PATH_SEPARATOR);

        return matcher.match(mockPath, inboundPath);
    }

    public static Optional<String> convertInputStreamToString(final InputStream inputStream,
                                                              final boolean closeStream) {

        try {
            return Optional.of(IOUtils.toString(inputStream, Charset.defaultCharset()));
        } catch (IOException ex) {
            logger.error("Error reading input stream to string", ex);
            return Optional.empty();
        } finally {
            if (closeStream) {
                GeneralUtils.closeSilently(inputStream);
            }
        }

    }

    public static String base64Encode(final byte[] plainContentBytes) {

        if (org.apache.commons.codec.binary.Base64.isBase64(plainContentBytes)) {
            return new String(plainContentBytes);
        }

        return Base64.getEncoder().encodeToString(plainContentBytes);
    }

    public static String base64Encode(final String plainContent) {

        if (org.apache.commons.codec.binary.Base64.isBase64(plainContent)) {
            return plainContent;
        }

        return Base64.getEncoder().encodeToString(plainContent.getBytes());
    }

    public static String base64Decode(final String encodedContent) {

        if (!org.apache.commons.codec.binary.Base64.isBase64(encodedContent)) {
            return encodedContent;
        }

        return new String(Base64.getDecoder().decode(encodedContent.getBytes()));
    }

    public static void closeSilently(final InputStream fis) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException ex) {
                logger.error("Error closing Inputstream", ex);
            }
        }
    }

    public static void executeAfterTransactionCommits(final Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    public static String extractRequestBody(HttpServletRequest request) {
        try {
            return IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException e) {
            logger.error("Error reading request body", e);
            return null;
        }
    }


    public static String[] splat(HttpServletRequest request, String routeMapping) {

        String pathInfo = request.getRequestURI();

        // Remove the application context if needed
        String contextPath = request.getContextPath();
        if (contextPath != null && pathInfo.startsWith(contextPath)) {
            pathInfo = pathInfo.substring(contextPath.length());
        }

        // Transform the route pattern into a Regex
        // Escape special character and transform '*' into capture groups (.*?)
        String regex = "^" + Pattern.quote(routeMapping)
                .replace("*", "\\E(.*)\\Q") + "$";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(pathInfo);

        if (matcher.find()) {
            List<String> splats = new ArrayList<>();
            for (int i = 1; i <= matcher.groupCount(); i++) {
                splats.add(matcher.group(i));
            }
            return splats.toArray(new String[0]);
        }

        return new String[0];
    }

}
