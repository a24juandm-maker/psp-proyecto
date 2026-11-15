package org.example;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    public static String sha256(String texto) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] resultado = sha256.digest(texto.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexadecimal = new StringBuilder();
        for (byte dato : resultado) {
            hexadecimal.append(String.format("%02x", dato & 0xff));
        }

        return hexadecimal.toString();
    }
}
