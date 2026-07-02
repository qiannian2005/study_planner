@echo off
setlocal

cd /d "%~dp0"

echo Starting frontend dev server...
npm run dev

pause
