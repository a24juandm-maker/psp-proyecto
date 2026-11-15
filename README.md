# Proyecto PSP: Mini blockchain

Proyecto Java de consola para construir una cadena de bloques sencilla.

## Estado actual

- `HashUtil` calcula hashes SHA-256 a partir de texto codificado en UTF-8.
- `Bloque` guarda los datos, el hash anterior, el nonce y calcula su propio hash con el formato `hashAnterior|datos|nonce`.
- `Main` muestra un ejemplo de creación de un primer bloque a partir de una semilla.

La búsqueda de nonce con dificultad y los procesos worker se añadirán en siguientes pasos.

## Abrir y ejecutar

Abre la carpeta del proyecto en IntelliJ IDEA y ejecuta la clase `org.example.Main`.

El proyecto usa Maven y está configurado para Java 17 o posterior.
