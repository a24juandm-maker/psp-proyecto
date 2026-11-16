package org.example;

import java.security.NoSuchAlgorithmException;

public class Worker {
    private static final int NUM_WORKERS = 4;

    public static void main(String[] args) throws NoSuchAlgorithmException {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Uso: Worker <hashAnterior> <datos> <dificultad> <id>");
        }

        String hashAnterior = args[0];
        String datos = args[1];
        int dificultad = Integer.parseInt(args[2]);
        int id = Integer.parseInt(args[3]);

        if (dificultad < 0 || dificultad > 64) {
            throw new IllegalArgumentException("La dificultad debe estar entre 0 y 64");
        }
        if (id < 0 || id >= NUM_WORKERS) {
            throw new IllegalArgumentException("El id debe estar entre 0 y 3");
        }

        long nonce = id;
        while (true) {
            Bloque bloque = new Bloque(datos, hashAnterior, nonce);
            if (bloque.cumpleDificultad(dificultad)) {
                System.out.println(bloque.getNonce() + "|" + bloque.getHash());
                return;
            }

            nonce += NUM_WORKERS;
        }
    }
}
