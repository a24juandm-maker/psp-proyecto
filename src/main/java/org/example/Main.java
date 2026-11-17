package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final int NUM_WORKERS = 4;

    public static void main(String[] args) {
        try (Scanner teclado = new Scanner(System.in)) {
            System.out.print("Introduce la semilla: ");
            String seed = teclado.nextLine().trim();
            if (seed.isEmpty()) {
                System.err.println("La seed no puede estar vacía.");
                return;
            }

            System.out.print("Introduce la dificultad (0-64): ");
            int dificultad;
            try {
                dificultad = Integer.parseInt(teclado.nextLine().trim());
            } catch (NumberFormatException e) {
                System.err.println("La dificultad debe ser un número entero.");
                return;
            }
            if (dificultad < 0 || dificultad > 64) {
                System.err.println("La dificultad debe estar entre 0 y 64.");
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

                for (int i = 0; i < bloques.size(); i++) {
                    String datos = bloques.get(i);
                    try {
                        Resultado resultado = minarBloque(hashAnterior, datos, dificultad);
                        Bloque bloque = resultado.getBloque();

                        System.out.println("\nBloque " + (i + 1));
                        System.out.println("Datos: " + bloque.getDatos());
                        System.out.println("PID worker ganador: " + resultado.getPid());
                        System.out.println("Hash anterior: " + bloque.getHashAnterior());
                        System.out.println("Nonce: " + bloque.getNonce());
                        System.out.println("Hash: " + bloque.getHash());
                        System.out.println("Validación: correcta");
                        System.out.println("Los workers han finalizado.");

                        hashAnterior = bloque.getHash();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Se interrumpió la minería del bloque.");
                        return;
                    }
                }
            } catch (InvalidPathException e) {
                System.err.println("La ruta indicada no es válida.");
            } catch (IOException e) {
                System.err.println("Error al procesar el fichero o los workers: " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                System.err.println("No se pudo calcular SHA-256.");
            }
        }
    }

    private static Resultado minarBloque(String hashAnterior, String datos, int dificultad)
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        List<Process> procesos = new ArrayList<>();
        ExecutorService lectores = Executors.newFixedThreadPool(NUM_WORKERS);
        CompletionService<Propuesta> resultados = new ExecutorCompletionService<>(lectores);

        try {
            String nombreJava = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "java.exe" : "java";
            String ejecutableJava = Path.of(System.getProperty("java.home"), "bin", nombreJava)
                    .toString();
            String classPath = System.getProperty("java.class.path");

            for (int id = 0; id < NUM_WORKERS; id++) {
                Process proceso = new ProcessBuilder(
                        ejecutableJava, "-cp", classPath, Worker.class.getName(),
                        hashAnterior, datos, String.valueOf(dificultad), String.valueOf(id))
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start();
                procesos.add(proceso);
                resultados.submit(() -> leerPropuesta(proceso));
            }

            for (int i = 0; i < NUM_WORKERS; i++) {
                Future<Propuesta> futuro = resultados.take();
                Propuesta propuesta;
                try {
                    propuesta = futuro.get();
                } catch (ExecutionException e) {
                    throw new IOException("No se pudo leer la propuesta de un worker.", e);
                }

                if (propuesta == null) {
                    continue;
                }
                if (!propuesta.isFormatoValido()) {
                    System.err.println("Se descarta una propuesta con formato incorrecto.");
                    continue;
                }

                Bloque candidato = new Bloque(datos, hashAnterior, propuesta.getNonce());
                boolean hashCorrecto = candidato.getHash().equals(propuesta.getHash());
                boolean dificultadCorrecta = candidato.cumpleDificultad(dificultad);
                if (hashCorrecto && dificultadCorrecta) {
                    return new Resultado(candidato, propuesta.getPid());
                }

                System.err.println("Se descarta una propuesta que no supera la validación.");
            }

            throw new IOException("Los workers terminaron sin proponer un bloque válido.");
        } finally {
            for (Process proceso : procesos) {
                if (proceso.isAlive()) {
                    proceso.destroy();
                }
            }
            for (Process proceso : procesos) {
                if (!proceso.waitFor(2, TimeUnit.SECONDS)) {
                    proceso.destroyForcibly();
                    proceso.waitFor();
                }
            }
            lectores.shutdownNow();
        }
    }

    private static Propuesta leerPropuesta(Process proceso) throws IOException {
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                proceso.getInputStream(), StandardCharsets.UTF_8))) {
            String linea = lector.readLine();
            if (linea == null) {
                return null;
            }

            String[] partes = linea.split("\\|", -1);
            if (partes.length != 2) {
                return new Propuesta(proceso.pid(), -1, "", false);
            }

            try {
                long nonce = Long.parseLong(partes[0]);
                return new Propuesta(proceso.pid(), nonce, partes[1], true);
            } catch (NumberFormatException e) {
                return new Propuesta(proceso.pid(), -1, "", false);
            }
        }
    }

    private static class Propuesta {
        private final long pid;
        private final long nonce;
        private final String hash;
        private final boolean formatoValido;

        private Propuesta(long pid, long nonce, String hash, boolean formatoValido) {
            this.pid = pid;
            this.nonce = nonce;
            this.hash = hash;
            this.formatoValido = formatoValido;
        }

        private long getPid() {
            return pid;
        }

        private long getNonce() {
            return nonce;
        }

        private String getHash() {
            return hash;
        }

        private boolean isFormatoValido() {
            return formatoValido;
        }
    }

    private static class Resultado {
        private final Bloque bloque;
        private final long pid;

        private Resultado(Bloque bloque, long pid) {
            this.bloque = bloque;
            this.pid = pid;
        }

        private Bloque getBloque() {
            return bloque;
        }

        private long getPid() {
            return pid;
        }
    }
}
