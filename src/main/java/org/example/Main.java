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
    private static final int DIFICULTAD = 2;

    public static void main(String[] args) {
        if (DIFICULTAD < 0 || DIFICULTAD > 64) {
            System.err.println("La dificultad debe estar entre 0 y 64.");
            return;
        }

        try (Scanner teclado = new Scanner(System.in)) {
            System.out.print("Introduce la semilla: ");
            String seed = teclado.nextLine().trim();
            if (seed.isEmpty()) {
                System.err.println("La semilla no puede estar vacía.");
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
                List<Bloque> cadena = new ArrayList<>();
                System.out.println("Hash inicial: " + hashAnterior);
                System.out.println("Bloques leídos: " + bloques.size());

                for (int i = 0; i < bloques.size(); i++) {
                    if (i > 0) {
                        System.out.println("\nPreparando el bloque " + (i + 1) + "...");
                    }
                    String datos = bloques.get(i);
                    try {
                        Resultado resultado = minarBloque(hashAnterior, datos, DIFICULTAD);
                        Bloque bloque = resultado.getBloque();
                        cadena.add(bloque);

                        System.out.println("\nBLOQUE " + (i + 1) + " VALIDADO");
                        System.out.println("Datos: " + bloque.getDatos());
                        System.out.println("Hash anterior: " + bloque.getHashAnterior());
                        System.out.println("Dificultad: " + DIFICULTAD);
                        System.out.println("Worker ganador: " + resultado.getWorkerId()
                                + " — PID: " + resultado.getPid());
                        System.out.println("Nonce encontrado: " + bloque.getNonce());
                        System.out.println("\nHash recibido:\n" + resultado.getHashRecibido());
                        System.out.println("\nHash recalculado por el principal:\n" + bloque.getHash());
                        System.out.println("\nValidación: hashes coincidentes y prefijo de "
                                + DIFICULTAD + " ceros correcto.");
                        System.out.println("Bloque incorporado a la cadena.");
                        System.out.println("Workers de esta búsqueda: todos finalizados.");

                        hashAnterior = bloque.getHash();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Se interrumpió la minería del bloque.");
                        return;
                    }
                }
                System.out.println("\nCadena terminada: " + cadena.size()
                        + " bloques incorporados.");
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
                int workerId = id;
                resultados.submit(() -> leerPropuesta(proceso, workerId));
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
                    return new Resultado(candidato, propuesta.getWorkerId(),
                            propuesta.getPid(), propuesta.getHash());
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
            boolean interrupcionPendiente = false;
            for (Process proceso : procesos) {
                boolean finalizado = false;
                while (!finalizado) {
                    try {
                        if (proceso.waitFor(2, TimeUnit.SECONDS)) {
                            finalizado = true;
                        } else {
                            proceso.destroyForcibly();
                            proceso.waitFor();
                            finalizado = true;
                        }
                    } catch (InterruptedException e) {
                        proceso.destroyForcibly();
                        interrupcionPendiente = true;
                    }
                }
            }
            lectores.shutdownNow();
            if (interrupcionPendiente) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static Propuesta leerPropuesta(Process proceso, int workerId) throws IOException {
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                proceso.getInputStream(), StandardCharsets.UTF_8))) {
            String linea = lector.readLine();
            if (linea == null) {
                return null;
            }

            String[] partes = linea.split("\\|", -1);
            if (partes.length != 2) {
                return new Propuesta(workerId, proceso.pid(), -1, "", false);
            }

            try {
                long nonce = Long.parseLong(partes[0]);
                return new Propuesta(workerId, proceso.pid(), nonce, partes[1], true);
            } catch (NumberFormatException e) {
                return new Propuesta(workerId, proceso.pid(), -1, "", false);
            }
        }
    }

    private static class Propuesta {
        private final int workerId;
        private final long pid;
        private final long nonce;
        private final String hash;
        private final boolean formatoValido;

        private Propuesta(int workerId, long pid, long nonce, String hash, boolean formatoValido) {
            this.workerId = workerId;
            this.pid = pid;
            this.nonce = nonce;
            this.hash = hash;
            this.formatoValido = formatoValido;
        }

        private int getWorkerId() {
            return workerId;
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
        private final int workerId;
        private final long pid;
        private final String hashRecibido;

        private Resultado(Bloque bloque, int workerId, long pid, String hashRecibido) {
            this.bloque = bloque;
            this.workerId = workerId;
            this.pid = pid;
            this.hashRecibido = hashRecibido;
        }

        private Bloque getBloque() {
            return bloque;
        }

        private int getWorkerId() {
            return workerId;
        }

        private long getPid() {
            return pid;
        }

        private String getHashRecibido() {
            return hashRecibido;
        }
    }
}
