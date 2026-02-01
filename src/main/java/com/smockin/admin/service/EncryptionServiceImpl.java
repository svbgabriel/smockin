package com.smockin.admin.service;

import org.jasypt.util.password.StrongPasswordEncryptor;
import org.springframework.stereotype.Service;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    private final StrongPasswordEncryptor passwordEncryptor;

    public EncryptionServiceImpl(StrongPasswordEncryptor passwordEncryptor) {
        this.passwordEncryptor = passwordEncryptor;
    }

    public String encrypt(final String passwordPlain) {
        return passwordEncryptor.encryptPassword(passwordPlain);
    }

    public boolean verify(final String passwordPlain, final String passwordEnc) {
        return passwordEncryptor.checkPassword(passwordPlain, passwordEnc);
    }

}
