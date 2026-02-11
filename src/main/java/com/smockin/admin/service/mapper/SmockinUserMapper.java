package com.smockin.admin.service.mapper;

import com.smockin.admin.dto.response.SmockinUserResponseDTO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.utils.GeneralUtils;
import org.springframework.stereotype.Component;

@Component
public class SmockinUserMapper {

    public SmockinUserResponseDTO toSmockinUserResponseDTO(final SmockinUser u) {
        return new SmockinUserResponseDTO(
                u.getExtId(),
                (isPasswordResetTokenValid(u)) ? u.getPasswordResetToken() : null,
                u.getDateCreated(),
                u.getUsername(),
                u.getFullName(),
                u.getRole());
    }

    private boolean isPasswordResetTokenValid(final SmockinUser user) {
        return (user.getPasswordResetToken() != null
                && user.getPasswordResetTokenExpiry() != null
                && GeneralUtils.getCurrentDate().before(user.getPasswordResetTokenExpiry()));
    }

}
