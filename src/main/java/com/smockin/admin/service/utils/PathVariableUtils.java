package com.smockin.admin.service.utils;

import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

@Component
public class PathVariableUtils {

    public String formatInboundPathVarArgs(final String inboundPath) {

        if (StringUtils.isBlank(inboundPath)) {
            return null;
        }

        final int varArgStart = Strings.CS.indexOf(inboundPath, ":");

        if (varArgStart > -1) {

            final int varArgEnd = Strings.CS.indexOf(inboundPath, GeneralUtils.URL_PATH_SEPARATOR, varArgStart);

            final String varArg = (varArgEnd > -1)
                    ? StringUtils.substring(inboundPath, varArgStart, varArgEnd)
                    : StringUtils.substring(inboundPath, varArgStart);

            final String result = Strings.CS.replace(inboundPath, varArg, "{" + StringUtils.remove(varArg, ':') + "}");

            return formatInboundPathVarArgs(result);
        }

        return inboundPath;
    }

    public String formatOutboundPathVarArgs(final String outboundPath) {

        if (StringUtils.isBlank(outboundPath)) {
            return null;
        }

        final int varArgStart = Strings.CS.indexOf(outboundPath, "{");

        if (varArgStart > -1) {

            final int varArgEnd = Strings.CS.indexOf(outboundPath, "}", varArgStart);

            final String varArg = (varArgEnd > -1)
                    ? StringUtils.substring(outboundPath, varArgStart, varArgEnd + 1)
                    : StringUtils.substring(outboundPath, varArgStart);

            final String result = Strings.CS.replace(outboundPath, varArg,
                    ":" + StringUtils.remove(StringUtils.remove(varArg, '{'), '}'));

            return formatOutboundPathVarArgs(result);
        }

        return outboundPath;
    }

}
