package com.smockin.mockserver.service.bean;

import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by mgallina on 11/08/17.
 */
public record ProxiedKey(String path, RestMethodEnum method) {

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof ProxiedKey(String path1, RestMethodEnum method1))) {
            return false;
        }

        return new EqualsBuilder()
                .append(path, path1)
                .append(method, method1)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(path)
                .append(method)
                .toHashCode();
    }

}
