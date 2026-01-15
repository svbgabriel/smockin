package com.smockin.admin.dto;

import com.smockin.admin.enums.MockImportKeepStrategyEnum;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MockImportConfigDTO {

    private boolean keepExisting;
    private MockImportKeepStrategyEnum keepStrategy;

    public MockImportConfigDTO() {

    }

    public MockImportConfigDTO(MockImportKeepStrategyEnum keepStrategy) {
        this.keepExisting = true;
        this.keepStrategy = keepStrategy;
    }

}
