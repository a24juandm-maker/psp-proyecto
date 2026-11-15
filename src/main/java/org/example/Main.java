package org.example;

public class Main {
    public static void main(String[] args) throws Exception {
        String semilla = "semilla-ejemplo";
        String hashInicial = HashUtil.sha256(semilla);

        Bloque bloque = new Bloque("Primer bloque", hashInicial, 0);

        System.out.println("Hash inicial: " + hashInicial);
        System.out.println("Datos: " + bloque.getDatos());
        System.out.println("Hash del bloque: " + bloque.getHash());
    }
}
