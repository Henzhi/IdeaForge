# IdeaForge 启动脚本(加载 .env 中所有环境变量后启动)
# 用法: .\start.ps1

$ErrorActionPreference = "Stop"

# 1. 加载 .env
Write-Host "加载 .env 配置..." -ForegroundColor Cyan
Get-Content .env | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $kv = $line.Split("=", 2)
        if ($kv.Count -eq 2) {
            $key = $kv[0].Trim()
            $val = $kv[1].Trim()
            [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
        }
    }
}

# 2. 停旧进程
Stop-Process -Name java -Force -ErrorAction SilentlyContinue

# 3. 检查 Key
if (!$env:LLM_API_KEY -or $env:LLM_API_KEY -eq "sk-your-deepseek-key") {
    Write-Host "警告: LLM_API_KEY 未设置或为默认值,故事生成将不可用" -ForegroundColor Yellow
}

# 4. 启动
Write-Host "启动 IdeaForge..." -ForegroundColor Green
java "-Dfile.encoding=UTF-8" "-DREDIS_HOST=127.0.0.1" "-DREDIS_PORT=6380" -jar ideaforge-app\target\ideaforge-app-0.1.0-SNAPSHOT.jar
