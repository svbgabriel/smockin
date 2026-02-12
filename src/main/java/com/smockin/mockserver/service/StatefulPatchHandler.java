package com.smockin.mockserver.service;

import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StatefulPatchHandler {

    public void patchAddOperation(final String path, final Map<String, Object> matchedMap, final Object value, final boolean canOverwriteExisting) {

        if (path.contains(GeneralUtils.URL_PATH_SEPARATOR)) {

            final String[] paths = path.split(GeneralUtils.URL_PATH_SEPARATOR);

            Object obj = matchedMap;

            for (int i=0; i < paths.length; i++) {

                final String p = paths[i];
                final boolean lastIteration = (i == (paths.length - 1));

                if (obj instanceof List l) {

                    final int indx = NumberUtils.toInt(p, -1);

                    if (indx == -1) {
                        throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                    }

                    if (lastIteration) {

                        if (l.size() < indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.PATH_OUT_OF_RANGE_LIST_INDEX, path, indx));
                        }

                        if (!l.isEmpty()
                                && !l.getFirst().getClass().equals(value.getClass())) {
                            throw new StatefulValidationException(
                                    String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION,
                                            "'value' in path '" + path + "' has an incompatible data type with existing values in list"));
                        }

                        l.add(indx, value);

                    } else {

                        if (l.size() <= indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.PATH_OUT_OF_RANGE_LIST_INDEX, path, indx));
                        }

                        obj = l.get(indx);

                    }

                } else if (obj instanceof Map map) {

                    if (lastIteration) {

                        if (!canOverwriteExisting && map.containsKey(p)) {
                            throw new StatefulValidationException(
                                    String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'path' value '" + path + "' already exists"));
                        }

                        map.put(p, value);
                    } else {
                        obj = map.get(p);
                    }

                } else {
                    throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                }

            }

        } else {

            if (matchedMap.get(path) instanceof List l) {

                if (!l.isEmpty()
                        && !l.getFirst().getClass().equals(value.getClass())) {
                    throw new StatefulValidationException(
                            String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION,
                                    "'value' in path '" + path + "' has an incompatible data type with existing values in list"));
                }

                l.add(value);

            } else {

                // Map, String, Int, etc...

                if (!canOverwriteExisting && matchedMap.containsKey(path)) {
                    throw new StatefulValidationException(
                            String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'path' value '" + path + "' already exists"));
                }

                matchedMap.put(path, value);
            }

        }

    }

    public void patchRemoveOperation(final String path, final Map<String, Object> matchedMap) {

        if (path.contains(GeneralUtils.URL_PATH_SEPARATOR)) {

            final String[] paths = path.split(GeneralUtils.URL_PATH_SEPARATOR);

            Object obj = matchedMap;

            for (int i=0; i < paths.length; i++) {

                final String p = paths[i];
                final boolean lastIteration = (i == (paths.length - 1));

                if (obj instanceof List l) {

                    if (lastIteration && "-".equals(p)) {

                        l.removeFirst();

                    } else {

                        final int indx = NumberUtils.toInt(p, -1);

                        if (indx == -1) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                        }

                        if (l.size() <= indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.PATH_OUT_OF_RANGE_LIST_INDEX, path, indx));
                        }

                        if (lastIteration) {
                            l.remove(indx);
                        } else {
                            obj = l.get(indx);
                        }

                    }

                } else if (obj instanceof Map map) {

                    if (lastIteration) {

                        if (!map.containsKey(p)) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }

                        map.remove(p);
                    } else {
                        obj = map.get(p);
                    }

                } else {
                    throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                }

            }

        } else {

            if (!matchedMap.containsKey(path)) {
                throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
            }

            matchedMap.remove(path);

        }

    }

    public void patchCopyOperation(final String from, final Map<String, Object> matchedMap, final String path) {

        if (from.contains(GeneralUtils.URL_PATH_SEPARATOR)) {

            final String[] fromPaths = from.split(GeneralUtils.URL_PATH_SEPARATOR);

            Object obj = matchedMap;

            for (int i=0; i < fromPaths.length; i++) {

                final String fp = fromPaths[i];
                final boolean lastIteration = (i == (fromPaths.length - 1));

                if (obj instanceof List l) {

                    final int indx = NumberUtils.toInt(fp, -1);

                    if (indx == -1) {
                        throw new StatefulValidationException(String.format(StatefulValidationException.FROM_STRUCTURE_MISALIGN, from));
                    }

                    if (lastIteration) {

                        if (l.size() < indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.FROM_OUT_OF_RANGE_LIST_INDEX, from, indx));
                        }

                        if (l.get(indx) == null) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }

                        patchAddOperation(path, matchedMap, l.get(indx), true);

                    } else {

                        if (l.size() <= indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.FROM_OUT_OF_RANGE_LIST_INDEX, from, indx));
                        }

                        obj = l.get(indx);

                    }

                } else if (obj instanceof Map map) {

                    if (lastIteration) {

                        if (!map.containsKey(fp)) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }

                        patchAddOperation(path, matchedMap, map.get(from), true);

                    } else {
                        obj = map.get(fp);
                    }

                } else {
                    throw new StatefulValidationException(String.format(StatefulValidationException.FROM_STRUCTURE_MISALIGN, from));
                }

            }

        } else {

            if (!matchedMap.containsKey(from)) {
                throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
            }

            patchAddOperation(path, matchedMap, matchedMap.get(from), true);

        }

    }

    public void patchMoveOperation(final String from, final Map<String, Object> matchedMap, final String path) {

        if (from.contains(GeneralUtils.URL_PATH_SEPARATOR)) {

            final String[] fromPaths = from.split(GeneralUtils.URL_PATH_SEPARATOR);

            Object obj = matchedMap;

            for (int i=0; i < fromPaths.length; i++) {

                final String fp = fromPaths[i];
                final boolean lastIteration = (i == (fromPaths.length - 1));

                if (obj instanceof List l) {

                    final int indx = NumberUtils.toInt(fp, -1);

                    if (indx == -1) {
                        throw new StatefulValidationException(String.format(StatefulValidationException.FROM_STRUCTURE_MISALIGN, from));
                    }

                    if (lastIteration) {

                        if (l.size() < indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.FROM_OUT_OF_RANGE_LIST_INDEX, from, indx));
                        }

                        if (l.get(indx) == null) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }

                        patchAddOperation(path, matchedMap, l.get(indx), true);
                        patchRemoveOperation(from, matchedMap);

                    } else {

                        if (l.size() <= indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.FROM_OUT_OF_RANGE_LIST_INDEX, from, indx));
                        }

                        obj = l.get(indx);

                    }

                } else if (obj instanceof Map map) {

                    if (lastIteration) {

                        if (!map.containsKey(fp)) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }

                        patchAddOperation(path, matchedMap, map.get(from), true);
                        patchRemoveOperation(from, matchedMap);

                    } else {
                        obj = map.get(fp);
                    }

                } else {
                    throw new StatefulValidationException(String.format(StatefulValidationException.FROM_STRUCTURE_MISALIGN, from));
                }

            }

        } else {

            if (!matchedMap.containsKey(from)) {
                throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
            }

            patchAddOperation(path, matchedMap, matchedMap.get(from), true);
            patchRemoveOperation(from, matchedMap);

        }

    }

    public void addReplaceOperation(final String path, final Map<String, Object> matchedMap, final Object value) {

        if (path.contains(GeneralUtils.URL_PATH_SEPARATOR)) {

            final String[] paths = path.split(GeneralUtils.URL_PATH_SEPARATOR);

            Object obj = matchedMap;

            for (int i=0; i < paths.length; i++) {

                final String p = paths[i];
                final boolean lastIteration = (i == (paths.length - 1));

                if (obj instanceof List l) {

                    final int indx = NumberUtils.toInt(p, -1);

                    if (indx == -1) {
                        throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                    }

                    if (l.size() <= indx) {
                        throw new StatefulValidationException(String.format(StatefulValidationException.PATH_OUT_OF_RANGE_LIST_INDEX, path, indx));
                    }

                    if (lastIteration) {

                        if (!l.isEmpty()
                                && !l.getFirst().getClass().equals(value.getClass())) {
                            throw new StatefulValidationException("'value' in path '" + path + "' has an incompatible data type with existing values in list");
                        }

                        l.set(indx, value);

                    } else {

                        if (l.size() <= indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.PATH_OUT_OF_RANGE_LIST_INDEX, path, indx));
                        }

                        obj = l.get(indx);

                    }

                } else if (obj instanceof Map map) {

                    if (lastIteration) {

                        if (!map.containsKey(p)) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }
                        if (!map.get(p).getClass().equals(value.getClass())) {
                            throw new StatefulValidationException("'value' in path '" + path + "' has an incompatible data type with existing value");
                        }

                        map.remove(p);
                        map.put(p, value);

                    } else {
                        obj = map.get(p);
                    }

                } else {
                    throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                }

            }

        } else {

            if (!matchedMap.containsKey(path)) {
                throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
            }
            if (!matchedMap.get(path).getClass().equals(value.getClass())) {
                throw new StatefulValidationException("'value' in path '" + path + "' has an incompatible data type with existing value");
            }

            matchedMap.put(path, value);

        }

    }

}
