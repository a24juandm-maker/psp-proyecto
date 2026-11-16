# Proyecto PSP: Mini blockchain

Proyecto Java de consola para construir una cadena de bloques sencilla.

## Estado actual

- `HashUtil` calcula hashes SHA-256 a partir de texto codificado en UTF-8.
- También comprueba que un hash comience con el número de ceros indicado por la dificultad.
- `Bloque` guarda los datos, el hash anterior, el nonce y calcula su propio hash con el formato `hashAnterior|datos|nonce`.
- `Worker` busca un nonce usando una secuencia propia y comunica la primera solución como `nonce|hash`.
- `Main` pide la seed y la ruta del fichero, valida ambas entradas y muestra el hash inicial y el número de bloques leídos.

La coordinación de los cuatro workers desde `Main` se añadirá en siguientes pasos.

## Abrir y ejecutar

Abre la carpeta del proyecto en IntelliJ IDEA y ejecuta la clase `org.example.Main`.

El proyecto usa Maven y está configurado para Java 17 o posterior.
