package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by mgallina.
 */
@Setter
@Getter
public class SmockinNewUserDTO extends SmockinUserDTO {

    private String password;

    public SmockinNewUserDTO() {
    }

    public SmockinNewUserDTO(String username, String fullName, SmockinUserRoleEnum role, String password) {
        super(username, fullName, role);
        this.password = password;
    }

}
