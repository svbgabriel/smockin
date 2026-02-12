package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockStatefulMeta;
import com.smockin.utils.GeneralUtils;
import lombok.Getter;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class StatefulJsonHandler {

    public boolean isComplexJsonStructure(final String fieldIdPathPattern) {
        return fieldIdPathPattern != null
                && fieldIdPathPattern.contains(".");
    }

    public void appendIdToJson(final Map<String, Object> jsonDataMap,
                               final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (isComplexJsonStructure(fieldIdPathPattern)) {

            final String[] pathArray = StringUtils.split(fieldIdPathPattern, ".");

            int index = 0;
            Object currentJsonObject = jsonDataMap;

            for (String e : pathArray) {

                if (currentJsonObject == null) {
                    break;
                }

                final Optional<Object> currentJsonObjectOpt = appendIdToJsonIdLocator(currentJsonObject, index, e, pathArray.length);

                if (currentJsonObjectOpt.isEmpty()) {
                    break;
                }

                currentJsonObject = currentJsonObjectOpt.get();

                index++;
            }

        } else {

            final String fieldId = restfulMockStatefulMeta.getIdFieldName();

            // Append ID if none present
            jsonDataMap.computeIfAbsent(fieldId, k -> GeneralUtils.generateUUID());

        }

    }

    private Optional<Object> appendIdToJsonIdLocator(Object currentJsonObject,
                                                     final int index,
                                                     final String path,
                                                     final int pathArrayLength) {

        Map<String, Object> currentJsonObjectMap = null;

        if (currentJsonObject instanceof Map) {

            currentJsonObjectMap = (Map<String, Object>) currentJsonObject;

        } else if (currentJsonObject instanceof List) {

            final List<Map<String, Object>> currentJsonObjectList = (List<Map<String, Object>>) currentJsonObject;

            if (!currentJsonObjectList.isEmpty()) {
                currentJsonObjectMap = currentJsonObjectList.getFirst();
            }

        }

        if (index == (pathArrayLength - 1)) {

            if (currentJsonObjectMap != null) {
                currentJsonObjectMap.computeIfAbsent(path, k -> GeneralUtils.generateUUID());
            }

            return Optional.empty();
        }

        if (currentJsonObjectMap != null && currentJsonObjectMap.containsKey(path)) {
            return Optional.of(currentJsonObjectMap.get(path));
        }

        return Optional.of(currentJsonObject);
    }

    public Optional<Map<String, Object>> findDataStateRecord(
            final List<Map<String, Object>> allStateData,
            final String fieldIdPathPattern,
            final String targetId) {

        final String[] pathArray = StringUtils.split(fieldIdPathPattern, ".");

        final Optional<StatefulPath> jsonPathOpt = findDataStateRecordPath(allStateData, pathArray, targetId);

        if (jsonPathOpt.isEmpty()) {
            return Optional.empty();
        }

        return findDataStateRecordByPath(allStateData, jsonPathOpt.get().path());
    }

    public Optional<Map<String, Object>> findDataStateRecordByPath(
            final List<Map<String, Object>> allStateDataSrc,
            final String path) {

        final List<Map<String, Object>> allStateDataCopy
                = SerializationUtils.clone(new StatefulSearchData(allStateDataSrc)).data();

        final String[] pathArray = StringUtils.split(path, ".");

        Map<String, Object> mainDataRecord = null;
        Object currentDataRecordObject = null;

        for (String p : pathArray) {

            if (mainDataRecord == null) {
                final Integer arrayPosition = extractArrayPosition(p);
                mainDataRecord = allStateDataCopy.get(arrayPosition);
                currentDataRecordObject = mainDataRecord;
                continue;
            }

            if (mainDataRecord == null || currentDataRecordObject == null) {
                return Optional.empty();
            }

            if (p.contains("[") && p.contains("]")) {

                // List

                final Integer arrayPosition = extractArrayPosition(p);

                if (arrayPosition == null) {
                    return Optional.empty();
                }

                Iterator<Object> dataRecordListItr = ((List<Object>) currentDataRecordObject).iterator();

                int dataRecordListItrIdx = 0;

                while (dataRecordListItr.hasNext()) {

                    Object o = dataRecordListItr.next();

                    if (arrayPosition != dataRecordListItrIdx) {
                        dataRecordListItr.remove();
                    } else {
                        currentDataRecordObject = o;
                    }

                    dataRecordListItrIdx++;
                }

            } else if (p.contains("=")) {

                // ID matching

                final String[] args = StringUtils.split(p, "=");
                final String idName = args[0];
                final String idValue = args[1];

                final String actualIdValue = (String) ((Map<String, Object>) currentDataRecordObject).get(idName);

                if (!Strings.CS.equals(idValue, actualIdValue)) {
                    return Optional.empty();
                }

            } else {

                // Map

                currentDataRecordObject = ((Map<String, Object>) currentDataRecordObject).get(p);

            }

        }

        return Optional.ofNullable(mainDataRecord);
    }

    public void removeDataStateRecordByPath(
            final List<Map<String, Object>> allStateDataSrc,
            final String path) {

        final int lastArrayEndPos = path.lastIndexOf("].");

        if (lastArrayEndPos == -1) {
            return;
        }

        final String amendedPath = StringUtils.substring(path, 0, (lastArrayEndPos + 1));
        final String[] pathArray = StringUtils.split(amendedPath, ".");

        Object currentDataRecordObject = null;

        for (int i = 0; i < pathArray.length; i++) {

            final String p = pathArray[i];

            if (i == 0) {
                final Integer arrayPosition = extractArrayPosition(p);
                currentDataRecordObject = allStateDataSrc.get(arrayPosition);
                continue;
            }

            if (currentDataRecordObject == null) {
                return;
            }

            if (p.contains("[") && p.contains("]")) {

                // List

                final Integer arrayPosition = extractArrayPosition(p);

                if (arrayPosition == null) {
                    return;
                }

                if (i == (pathArray.length - 1)) {
                    ((List<Object>) currentDataRecordObject).remove(arrayPosition.intValue());
                } else {
                    currentDataRecordObject = ((List<Object>) currentDataRecordObject).get(arrayPosition);
                }

            } else {

                // Map

                currentDataRecordObject = ((Map<String, Object>) currentDataRecordObject).get(p);

            }

        }

    }

    public Integer extractArrayPosition(final String pathElement) {

        if (StringUtils.isBlank(pathElement)) {
            return null;
        }

        final String s1 = Strings.CS.remove(pathElement, "[");
        final String s2 = Strings.CS.remove(s1, "]");
        final int result = NumberUtils.toInt(s2, -1);

        return (result != -1) ? result : null;
    }

    public Optional<StatefulPath> findDataStateRecordPath(
            final List<Map<String, Object>> allStateData,
            final String[] pathArray,
            final String targetId) {

        final StatefulSearchPathResult result = new StatefulSearchPathResult();

        int index = 0;

        for (Map<String, Object> m : allStateData) {

            final int thisIndex = index;

            findStateIndex(pathArray, 0, targetId, m, result, "[" + index++ + "]");

            if (result.getPath().isPresent()) {
                return Optional.of(new StatefulPath(result.getPath().get(), thisIndex));
            }
        }

        return Optional.empty();
    }

    private void findStateIndex(
            final String[] pathArray,
            final int pathLevel,
            final String targetId,
            final Object currentJsonObject,
            final StatefulSearchPathResult result,
            final String myPath) {

        if (currentJsonObject == null) {
            return;
        }

        if (currentJsonObject instanceof String) {

            if (pathLevel == pathArray.length
                    && Strings.CS.equals(targetId, (String) currentJsonObject)) {
                result.path = Optional.of(myPath + "=" + currentJsonObject);
            }

        } else if (currentJsonObject instanceof Map) {

            if (pathLevel == pathArray.length) {
                return;
            }

            final String currentField = pathArray[pathLevel];

            Map<String, Object> data = ((Map<String, Object>) currentJsonObject);

            findStateIndex(pathArray, pathLevel + 1, targetId, data.get(currentField), result, myPath + "." + currentField);

        } else if (currentJsonObject instanceof List) {

            if (pathLevel == pathArray.length) {
                return;
            }

            final List<Map<String, Object>> currentJsonObjectList = (List<Map<String, Object>>) currentJsonObject;

            if (currentJsonObjectList.isEmpty()) {
                return;
            }

            int index = 0;
            for (Map<String, Object> mapElement : currentJsonObjectList) {
                findStateIndex(pathArray, pathLevel, targetId, mapElement, result, myPath + ".[" + (index++) + "]");
            }

        }

    }

    @Getter
    private static class StatefulSearchPathResult {

        private Optional<String> path = Optional.empty();

    }

    public record StatefulPath(String path, Integer index) {

    }

    public record StatefulSearchData(List<Map<String, Object>> data) implements Serializable {

    }

}
