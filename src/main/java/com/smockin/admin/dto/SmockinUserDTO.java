package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by mgallina.
 */
@Setter
@Getter
public class SmockinUserDTO {

    private String username;
    private String fullName;
    private SmockinUserRoleEnum role;

    public SmockinUserDTO() {

    }

    public SmockinUserDTO(String username, String fullName, SmockinUserRoleEnum role) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

}
