@echo off
REM ============================================================
REM  subir_a_github.bat
REM  Sube el proyecto InventarioGrupoIntelecto a GitHub con la
REM  estructura de carpetas correcta, usando Git.
REM
REM  USO:
REM    1. Coloca este archivo DENTRO de la carpeta
REM       InventarioGrupoIntelecto (junto a settings.gradle).
REM    2. Edita la línea "set REPO_URL=" de abajo con la URL de
REM       TU repositorio (créalo vacío en github.com primero,
REM       SIN marcar "Add a README file").
REM    3. Doble clic para ejecutar. La primera vez, Git te
REM       pedirá iniciar sesión en GitHub en una ventana.
REM ============================================================

set REPO_URL=https://github.com/cespinoza-hub/proff.git

cd /d "%~dp0"

if not exist settings.gradle (
    echo ERROR: No se encontro settings.gradle en esta carpeta.
    echo Este script debe estar DENTRO de la carpeta InventarioGrupoIntelecto.
    pause
    exit /b 1
)

where git >nul 2>nul
if errorlevel 1 (
    echo ERROR: Git no esta instalado.
    echo Descargalo de https://git-scm.com/download/win e instalalo con las opciones por defecto.
    pause
    exit /b 1
)

echo Subiendo proyecto a %REPO_URL% ...
if exist .git rmdir /s /q .git
git init
git add -A
git commit -m "Proyecto InventarioGrupoIntelecto completo"
git branch -M main
git remote add origin %REPO_URL%
git push -u origin main --force

if errorlevel 1 (
    echo.
    echo Hubo un error en el push. Revisa que la URL del repositorio sea correcta
    echo y que hayas iniciado sesion cuando Git lo pidio.
) else (
    echo.
    echo LISTO. Ve a la pestana Actions de tu repositorio en GitHub:
    echo el APK se compilara automaticamente en unos minutos.
)
pause
