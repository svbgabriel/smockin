package com.smockin.admin.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "USER_KEY_VALUE_DATA", uniqueConstraints={
        @UniqueConstraint(columnNames = {"USER_KEY", "CREATED_BY"})
})
@Data
@EqualsAndHashCode(callSuper=false)
public class UserKeyValueData extends Identifier {

    @Column(name = "USER_KEY", nullable = false, length = 50)
    private String key;

    @Column(name = "USER_VALUE", nullable = false, columnDefinition = "TEXT")
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY", nullable = false)
    private SmockinUser createdBy;

}
