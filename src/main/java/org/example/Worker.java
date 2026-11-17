package org.example;

import java.security.NoSuchAlgorithmException;

public class Worker {
    private static final int NUM_WORKERS = 4;

    public static void main(String[] args) throws NoSuchAlgorithmException {
        if (args.length != 4) {
            System.err.println("Uso: Worker <hashAnterior> <datos> <dificultad> <id>");
            return;
        }

        String hashAnterior = args[0];
        String datos = args[1];
        if (!hashAnterior.matches("[0-9a-fA-F]{64}")) {
            System.err.println("El hash anterior debe tener 64 caracteres hexadecimales.");
            return;
        }

        int dificultad;
        int id;
        try {
            dificultad = Integer.parseInt(args[2]);
            id = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.err.println("La dificultad y el id deben ser números enteros.");
            return;
        }

        if (dificultad < 0 || dificultad > 64) {
            System.err.println("La dificultad debe estar entre 0 y 64.");
            return;
        }
        if (id < 0 || id >= NUM_WORKERS) {
            System.err.println("El id debe estar entre 0 y 3.");
            return;
        }

        // Cada proceso recorre los nonces id, id + 4, id + 8, etc.
        long nonce = id;
        while (true) {
            Bloque bloque = new Bloque(datos, hashAnterior, nonce);
            if (bloque.cumpleDificultad(dificultad)) {
                System.out.println(bloque.getNonce() + "|" + bloque.getHash());
                return;
            }

            if (nonce > Long.MAX_VALUE - NUM_WORKERS) {
                System.err.println("El worker agotó su secuencia de nonces.");
                return;
            }
            nonce += NUM_WORKERS;
        }
    }
}
