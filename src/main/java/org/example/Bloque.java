package org.example;

import java.security.NoSuchAlgorithmException;

public class Bloque {
    private final String datos;
    private final String hashAnterior;
    private final long nonce;
    private final String hash;

    public Bloque(String datos, String hashAnterior, long nonce)
            throws NoSuchAlgorithmException {
        this.datos = datos;
        this.hashAnterior = hashAnterior;
        this.nonce = nonce;

        String contenido = hashAnterior + "|" + datos + "|" + nonce;
        this.hash = HashUtil.sha256(contenido);
    }

    public String getDatos() {
        return datos;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public long getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }
}
