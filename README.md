# Proyecto PSP: Mini blockchain

Aplicación Java de consola que construye una cadena sencilla de bloques a partir de las líneas de un fichero.

## Cómo funciona

- Se introduce una semilla y la ruta de un fichero de texto.
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

## Ejecución

Abre la carpeta del proyecto en IntelliJ IDEA y ejecuta `org.example.Main`. El programa solicita, en este orden, la semilla, la dificultad y la ruta del fichero. La dificultad debe ser un número entre 0 y 64. Para empezar, usa un valor bajo, como 2; cuanto mayor sea, más intentos de hash hacen falta.

El fichero debe contener una línea de texto por bloque. Puedes indicar `src/main/resources/datos-ejemplo.txt` para usar el ejemplo incluido.

El proyecto usa Maven y está configurado para Java 17 o posterior.
