@echo off
rem ShaderCreator starten (Windows)
cd /d "%~dp0"
where node >nul 2>nul
if %errorlevel%==0 (
  start "" http://localhost:8123
  node tools\serve.mjs 8123
  goto :eof
)
where python >nul 2>nul
if %errorlevel%==0 (
  echo Node.js nicht gefunden - nutze Python-Server auf http://localhost:8123
  start "" http://localhost:8123
  python -m http.server 8123
  goto :eof
)
echo Bitte Node.js (https://nodejs.org) oder Python 3 installieren.
pause
