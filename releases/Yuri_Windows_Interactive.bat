@echo off
title YURI AI - PERSONAL OFFLINE ASSISTANT
color 0B
cls
echo ==============================================================================
echo                YURI AI - OFFLINE ASSISTANT (JARVIS PROTOCOL)
echo ==============================================================================
echo.
echo Commander, offline systems are operational.
echo Connecting to local Qwen neural core at: http://127.0.0.1:8080
echo.
echo Testing local connection...
curl -s --connect-timeout 2 http://127.0.0.1:8080/health >nul 2>&1
if %errorlevel% neq 0 (
    echo [STATUS] Offline engine not detected at 127.0.0.1:8080.
    echo          Please make sure your local llama-server or Qwen is running:
    echo          Example: ./llama-server -m ~/models/qwen2.5.gguf --port 8080
    echo.
) else (
    echo [STATUS] Qwen 2.5 Neural Core: ONLINE (127.0.0.1:8080)
)
echo.
echo Opening Yuri Web Console...
start http://127.0.0.1:8080
echo.
echo [1] Type your directive below or press Enter to refresh.
echo [2] Type 'exit' to quit.
echo ==============================================================================
:loop
set /p "prompt=Commander > "
if "%prompt%"=="" goto loop
if /i "%prompt%"=="exit" exit /b

powershell -Command "$p='%prompt%'; $body = @{model='qwen2.5'; messages=@(@{role='user'; content=$p})} | ConvertTo-Json; try { $res = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/v1/chat/completions' -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 30; $reply = $res.choices[0].message.content; Write-Host \"`n[YURI]: $reply`n\" -ForegroundColor Cyan; Add-Type -AssemblyName System.Speech; $synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; $synth.Rate = 1; $synth.Speak($reply) } catch { Write-Host \"`n[YURI]: Offline engine at 127.0.0.1:8080 did not respond. Check your local server.`n\" -ForegroundColor Red }"
echo.
goto loop
