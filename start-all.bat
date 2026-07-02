@echo off
setlocal

cd /d "%~dp0"

start "Study Planner Backend" cmd /k call "%~dp0study_planner-main\start-backend.bat"
start "Study Planner Frontend" cmd /k call "%~dp0study-planner-frontend-main\start-frontend.bat"
