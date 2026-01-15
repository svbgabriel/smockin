package com.smockin.admin.dto.response;

import com.smockin.admin.dto.SmockinUserDTO;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class SmockinUserResponseDTO extends SmockinUserDTO {

    private String extId;
    private String passwordResetToken;
    private Date dateCreated;

    public SmockinUserResponseDTO() {
    }

    public SmockinUserResponseDTO(String extId, String passwordResetToken, Date dateCreated, String username, String fullName, SmockinUserRoleEnum role) {
        super(username, fullName, role);
        this.extId = extId;
        this.passwordResetToken = passwordResetToken;
        this.dateCreated = dateCreated;
    }

}
