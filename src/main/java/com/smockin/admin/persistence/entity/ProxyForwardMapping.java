package com.smockin.admin.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnDefault;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "PROXY_FORWARD_MAPPING")
@Data
@EqualsAndHashCode(callSuper=false)
public class ProxyForwardMapping extends Identifier {

    @ManyToOne
    @JoinColumn(name = "PROXY_FORWARD_USER_CONFIG_ID", nullable = false)
    private ProxyForwardUserConfig proxyForwardUserConfig;

    @Column(name = "PATH", length = 1000, nullable = false, unique = true)
    private String path;

    @Column(name = "PROXY_FORWARD_URL", length = 500, nullable = false)
    private String proxyForwardUrl;

    @ColumnDefault("false")
    @Column(name = "IS_DISABLED", nullable = false)
    private boolean disabled;

}
