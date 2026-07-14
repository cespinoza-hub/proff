# Inventario GrupoIntelecto

App Android (Kotlin) para toma de inventario mediante escaneo de códigos de barras y QR con la cámara.

## ⚠️ Importante sobre este entregable

Este entorno de trabajo **no tiene acceso al repositorio Maven de Google ni al SDK de Android**, por lo que no fue posible compilar aquí el archivo `.apk` binario. Lo que se entrega es el **proyecto completo de Android Studio, listo para compilar**, y hay **dos formas** de obtener el APK:

1. **En la nube, con GitHub Actions (recomendado si Android Studio te está dando problemas de sync/dependencias).** No requiere instalar nada. Ver sección "Compilar el APK con GitHub Actions" más abajo.
2. **Localmente en Android Studio.** Ver sección "Cómo generar el APK" más abajo.

## Compilar el APK con GitHub Actions (sin instalar nada)

El proyecto incluye `.github/workflows/build-apk.yml`, que compila automáticamente un APK de depuración usando la infraestructura de GitHub (que sí tiene acceso completo a los repositorios de Google/Maven, el SDK de Android y Gradle).

Pasos:

1. Crea un repositorio nuevo en [github.com](https://github.com) (puede ser privado o público).
2. Sube el contenido de esta carpeta (`InventarioGrupoIntelecto`) a ese repositorio. Dos formas de hacerlo:
   - **Con Git** (si lo tienes instalado):
     ```bash
     cd InventarioGrupoIntelecto
     git init
     git add .
     git commit -m "Proyecto inicial"
     git branch -M main
     git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
     git push -u origin main
     ```
   - **Sin Git**: en la página del repositorio en GitHub, usa el botón "Add file > Upload files" y arrastra toda la carpeta.
3. Ve a la pestaña **"Actions"** de tu repositorio en GitHub. El workflow **"Compilar APK"** se ejecutará automáticamente apenas subas el código (o puedes lanzarlo manualmente con el botón "Run workflow").
4. Espera unos 3-5 minutos a que el workflow termine con el ícono verde ✅.
5. Entra a esa ejecución del workflow y, en la sección **"Artifacts"** (al final de la página), descarga **"InventarioGrupoIntelecto-debug-apk"**. Es un .zip que contiene el `app-debug.apk` listo para instalar en cualquier celular Android (activando "Instalar apps de orígenes desconocidos").

Este APK de depuración es completamente funcional para pruebas e instalación directa. Si más adelante necesitas publicarlo en Google Play, se requiere generar una versión "release" firmada con una clave propia (puedo ayudarte a extender el workflow para eso cuando lo necesites).

## Requisitos para compilar

- Android Studio (versión reciente, Koala o superior recomendado)
- JDK 17 (viene incluido con Android Studio)
- Conexión a internet la primera vez (para descargar Gradle y las dependencias)

## Cómo generar el APK

1. Abre Android Studio → **File > Open** → selecciona la carpeta `InventarioGrupoIntelecto`.
2. Si Android Studio pregunta por el *Gradle Wrapper*, acepta que lo cree/repare automáticamente (el proyecto no incluye el binario `gradle-wrapper.jar`, pero Android Studio lo genera solo).
3. Espera a que termine el **Gradle Sync** (descarga automáticamente CameraX, ML Kit, Room, etc.).
4. Conecta un celular Android (modo desarrollador + depuración USB) o usa un emulador con cámara habilitada.
5. Presiona **Run ▶** para probar la app, o ve a **Build > Build Bundle(s) / APK(s) > Build APK(s)** para generar el instalable.
6. El archivo quedará en: `app/build/outputs/apk/debug/app-debug.apk`.
7. Para un APK firmado de producción: **Build > Generate Signed Bundle / APK**.

## Flujo de la app (mapeado a lo solicitado)

1. **Pantalla de entrada** (`WelcomeActivity`): pide *Nombre de usuario* y *Nombre de la empresa*, valida que no estén vacíos y los guarda en `SharedPreferences`.
2. **Pantalla principal** (`InventoryActivity`):
   - Campo para ingresar la **cantidad de artículos**.
   - Botón **"Siguiente"** que abre la cámara.
   - Al escanear, guarda automáticamente en la base de datos local (Room/SQLite): usuario, empresa, cantidad, código, tipo de código y fecha/hora.
   - Muestra el **listado en tiempo real** de todo lo ya escaneado (RecyclerView).
   - Cada fila tiene un ícono de **eliminar** (con confirmación) para borrar registros con fallas.
   - Botón **"Exportar a CSV"**.
3. **Pantalla de escaneo** (`ScannerActivity`): usa **CameraX** + **ML Kit Barcode Scanning** con `FORMAT_ALL_FORMATS`, por lo que detecta cualquier código que la cámara reconozca: QR, EAN-13, EAN-8, UPC-A, UPC-E, Code128, Code39, Code93, Codabar, Data Matrix, PDF417, Aztec, ITF, etc. También incluye un botón de **"Ingresar código manualmente"** como respaldo si la cámara no logra leer un código dañado.
4. **Exportación CSV** (`CsvExporter`): genera un archivo `usuario_yyyyMMdd_HHmmss.csv` y lo guarda en la carpeta pública **Documentos** del dispositivo (usa `MediaStore` en Android 10+, y escritura directa con permiso en Android 8-9). Al terminar, pregunta si deseas limpiar la base temporal.
5. **Reiniciar todo / comenzar desde cero**: en la pantalla principal, arriba a la derecha (ícono de refrescar en la barra superior), hay una opción **"Reiniciar todo"**. Pide confirmación y, si se acepta, borra todos los registros escaneados **y** los datos de usuario/empresa guardados, devolviendo a la app a la pantalla de entrada como si fuera recién instalada. Es la forma de empezar un inventario completamente nuevo.
   - Si en cambio solo quieres borrar los registros escaneados pero mantener la sesión de usuario/empresa activa, usa el botón de eliminar (🗑) en cada fila de la lista, uno por uno.

## Permisos utilizados

- `CAMERA`: para escanear.
- `WRITE_EXTERNAL_STORAGE` (solo hasta Android 9 / API 28): para escribir en Documentos. En Android 10+ no se requiere gracias a `MediaStore`.

## Notas técnicas / posibles ajustes

- `minSdk` está en **26** (Android 8.0) para simplificar los íconos adaptativos; se puede bajar a 21-23 si necesitas soportar equipos más antiguos (habría que agregar íconos PNG legacy en `mipmap-*dpi`).
- El separador del CSV es `;` (punto y coma), pensado para que Excel en español lo abra directamente en columnas. Si prefieres coma `,`, es un solo cambio en `CsvExporter.kt`.
- La base de datos es "temporal" en el sentido de que vive en el dispositivo mientras no la limpies; no hay sincronización con un servidor. Si más adelante necesitas respaldo en la nube o multiusuario simultáneo, se puede añadir un backend (Firebase, REST, etc.).
- Las versiones de librerías (CameraX 1.3.4, ML Kit Barcode Scanning 17.3.0, Room 2.6.1, AGP 8.5.0) son estables al momento de escribir esto; Android Studio puede sugerir actualizaciones al sincronizar, lo cual es seguro de aceptar.

## Estructura del proyecto

```
InventarioGrupoIntelecto/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/grupointelecto/inventario/
│       │   ├── WelcomeActivity.kt
│       │   ├── InventoryActivity.kt
│       │   ├── ScannerActivity.kt
│       │   ├── data/ (InventoryItem, InventoryDao, AppDatabase)
│       │   ├── adapter/ (InventoryAdapter)
│       │   └── util/ (CsvExporter)
│       └── res/ (layouts, values, drawables, iconos)
├── build.gradle
└── settings.gradle
```
