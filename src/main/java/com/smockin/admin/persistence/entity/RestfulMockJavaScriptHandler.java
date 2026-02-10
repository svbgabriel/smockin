package com.smockin.admin.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "REST_MOCK_JS_HANDLER")
@Data
@EqualsAndHashCode(callSuper=false)
public class RestfulMockJavaScriptHandler extends Identifier {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_MOCK_ID", nullable = false)
    private RestfulMock restfulMock;

    @Column(name = "SYNTAX", columnDefinition = "TEXT")
    private String syntax;

}
