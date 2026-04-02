package com.smockin.admin.ui.utils;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

public class UIFormattingUtils {

    public static String formatStatus(RecordStatusEnum status) {
        if (status == null) return "";
        return WordUtils.capitalizeFully(status.name());
    }

    public static String formatMockType(RestMockTypeEnum type) {
        if (type == null) return "";
        switch (type) {
            case SEQ: return "Sequential";
            case RULE: return "Rules";
            case PROXY_HTTP: return "Proxy HTTP";
            case PROXY_SSE: return "Proxy SSE";
            case PROXY_WS: return "Proxy WebSocket";
            case CUSTOM_JS: return "Custom JavaScript";
            case RULE_WS: return "Rules WebSocket";
            case STATEFUL: return "Stateful";
            default: return WordUtils.capitalizeFully(type.name().replace("_", " "));
        }
    }

    public static String formatMethod(RestMethodEnum method) {
        if (method == null) return "";
        return method.name(); // Methods are usually better in UPPERCASE (GET, POST)
    }

    public static String formatEnum(Enum<?> e) {
        if (e == null) return "";
        return formatString(e.name());
    }

    public static String formatString(String s) {
        if (s == null) return "";
        return WordUtils.capitalizeFully(s.replace("_", " "));
    }
}
