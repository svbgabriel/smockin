package com.smockin.mockserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailServerMessageInboxAttachmentLiteDTO {

    private String extId;
    private String name;
    private String mimeType;

}
