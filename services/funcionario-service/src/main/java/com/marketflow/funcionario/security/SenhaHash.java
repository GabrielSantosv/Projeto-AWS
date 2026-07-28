package com.marketflow.funcionario.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class SenhaHash {

    private SenhaHash() {
    }

    public static String gerar(String senha) {
        if (senha == null) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(senha.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel", exception);
        }
    }

    public static boolean verificar(String senha, String hashEsperado) {
        if (senha == null || hashEsperado == null) {
            return false;
        }
        return MessageDigest.isEqual(
                gerar(senha).getBytes(StandardCharsets.UTF_8),
                hashEsperado.getBytes(StandardCharsets.UTF_8)
        );
    }
}
