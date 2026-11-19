# Proyecto PSP: Mini blockchain

Aplicación Java de consola que construye una cadena sencilla de bloques a partir de las líneas de un fichero.

## Cómo funciona

- Se introduce una semilla y la ruta de un fichero de texto.
- La dificultad se configura con la constante `DIFICULTAD` de `Main` y se mantiene durante toda la ejecución.
- Cada línea no vacía del fichero se procesa como un bloque.
- El hash inicial se calcula con SHA-256 sobre la semilla.
- El hash de cada bloque se calcula como SHA-256 de `hashAnterior|datos|nonce`.
- Para buscar el nonce se lanzan cuatro procesos Worker, cada uno con una secuencia distinta.
- Main acepta una propuesta solo después de recalcular el hash y comprobar la dificultad.
- Cuando encuentra una propuesta válida, cierra los procesos restantes antes de continuar.

## Clases

- HashUtil: calcula hashes SHA-256 y comprueba el prefijo de ceros.
- Bloque: guarda los datos del bloque y calcula su hash.
- Worker: busca nonces en un proceso separado y devuelve nonce|hash.
- Main: valida las entradas, coordina los workers y muestra el resultado.
