package org.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final int DIFICULTAD = 2;

    public static void main(String[] args) {
        if (DIFICULTAD < 0 || DIFICULTAD > 64) {
            System.err.println("La dificultad debe estar entre 0 y 64.");
            return;
        }

        try (Scanner teclado = new Scanner(System.in)) {
            System.out.print("Introduce la seed: ");
            String seed = teclado.nextLine().trim();

            if (seed.isEmpty()) {
                System.err.println("La seed no puede estar vacía.");
                return;
            }

            System.out.print("Introduce la ruta del fichero: ");
            String rutaTexto = teclado.nextLine().trim();

            try {
                Path ruta = Path.of(rutaTexto);
                if (!Files.isRegularFile(ruta) || !Files.isReadable(ruta)) {
                    System.err.println("El fichero no existe o no se puede leer.");
                    return;
                }

                List<String> bloques = Files.readAllLines(ruta, StandardCharsets.UTF_8)
                        .stream()
                        .filter(linea -> !linea.isBlank())
                        .toList();

                if (bloques.isEmpty()) {
                    System.out.println("El fichero no contiene bloques.");
                    return;
                }

                String hashAnterior = HashUtil.sha256(seed);
                System.out.println("Hash inicial: " + hashAnterior);
                System.out.println("Bloques leídos: " + bloques.size());
                System.out.println("Dificultad: " + DIFICULTAD);
            } catch (InvalidPathException e) {
                System.err.println("La ruta indicada no es válida.");
            } catch (IOException e) {
                System.err.println("No se pudo leer el fichero: " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                System.err.println("No se pudo calcular SHA-256.");
            }
        }
    }
}
