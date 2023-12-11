cmake_minimum_required(VERSION 3.4.1) # Especifica la versión mínima de CMake

project("native_c")
# Crea y nombra la biblioteca, establece que es compartida,
# y proporciona las rutas relativas a sus fuentes.
add_library( # Establece el nombre de la biblioteca
        native-lib
        # Establece el tipo de biblioteca como una biblioteca compartida.
        SHARED
        # Proporciona una ruta relativa a tus archivos de código fuente.
        native-lib.cpp)

# Busca las librerías pre-construidas que necesitas y las enlaza a la biblioteca nativa.
find_package(oboe REQUIRED CONFIG)

target_link_libraries( # Especifica la biblioteca objetivo a enlazar.
        native-lib
        # Enlaza la biblioteca que has definido al archivo set_target_properties con las bibliotecas encontradas.
        # Aquí estamos utilizando Oboe y log library desde el NDK.
        oboe::oboe
        log)

# Especifica las rutas de inclusión donde el compilador puede buscar los archivos de encabezado.
target_include_directories(native-lib PRIVATE
        ${OBOE_DIR}/include)
