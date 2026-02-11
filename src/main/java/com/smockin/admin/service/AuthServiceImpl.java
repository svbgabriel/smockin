package com.smockin.admin.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.smockin.admin.config.JwtConfig;
import com.smockin.admin.dto.AuthDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.utils.GeneralUtils;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private JwtConfig jwtConfig;

    private Algorithm jwtAlgorithm;
    private JWTVerifier jwtVerifier;

    @PostConstruct
    public void init() {
        jwtAlgorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        jwtVerifier = JWT.require(jwtAlgorithm)
                .withIssuer(jwtConfig.getIssuer())
                .build();
    }

    @Override
    public String authenticate(final AuthDTO dto) throws ValidationException, AuthException {
        logger.debug("authenticate called");

        if (StringUtils.isBlank(dto.getUsername())) {
            throw new ValidationException("username is required");
        }

        if (StringUtils.isBlank(dto.getPassword())) {
            throw new ValidationException("password is required");
        }

        final SmockinUser user = smockinUserDAO.findByUsername(dto.getUsername());

        if (user == null
                || !encryptionService.verify(dto.getPassword(), user.getPassword())) {
            throw new AuthException();
        }

        final String token = generateJWT(user);

        user.setSessionToken(token);
        smockinUserDAO.save(user);

        return token;
    }

    @Override
    public void checkTokenRoles(final String jwt, SmockinUserRoleEnum... roles) throws AuthException {

        final DecodedJWT decodedJWT = jwtVerifier.verify(jwt);
        final Claim roleClaim = decodedJWT.getClaim(jwtConfig.getRoleKey());

        if (roleClaim == null || !Stream.of(roles).anyMatch(r -> r.name().equals(roleClaim.asString()))) {
            throw new AuthException();
        }
    }

    @Override
    public void verifyToken(final String jwt) throws AuthException {

        try {
            jwtVerifier.verify(jwt);
        } catch (JWTVerificationException ex) {
            logger.debug("JWT authorization failed", ex);
            throw new AuthException();
        }
    }

    String generateJWT(final SmockinUser user) {
        return JWT.create()
                .withIssuer(jwtConfig.getIssuer())
                .withClaim(jwtConfig.getRoleKey(), user.getRole().name())
                .withClaim(jwtConfig.getFullNameKey(), user.getFullName())
                .withClaim(jwtConfig.getUserNameKey(), user.getUsername())
                .withSubject(jwtConfig.getSubject())
                .withIssuedAt(GeneralUtils.getCurrentDate())
                .withExpiresAt(GeneralUtils.toDate(GeneralUtils.getCurrentDateTime().plusDays(99)))
                .sign(jwtAlgorithm);
    }

}
