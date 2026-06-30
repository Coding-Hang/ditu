param(
  [string]$Endpoint = "https://llm-fmljnd0r34k4pinm.cn-beijing.maas.aliyuncs.com/compatible-mode/v1/chat/completions",
  [string]$Model = "qwen3.7-max"
)

$ErrorActionPreference = "Stop"

if (-not $env:DITU_DEFAULT_MODEL_API_KEY) {
  $secure = Read-Host "Input model Authorization value" -AsSecureString
  $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  )
  $env:DITU_DEFAULT_MODEL_API_KEY = $plain
}

$body = @{
  model = $Model
  messages = @(
    @{ role = "system"; content = "You are a helpful assistant." },
    @{ role = "user"; content = "你是谁？" }
  )
} | ConvertTo-Json -Depth 8

# API_KEY 模式按当前已验证请求保持原始 Authorization 值，不自动追加 Bearer 前缀。
$response = Invoke-RestMethod `
  -Method Post `
  -Uri $Endpoint `
  -Headers @{ Authorization = $env:DITU_DEFAULT_MODEL_API_KEY; "Content-Type" = "application/json" } `
  -Body $body

$response.choices[0].message.content
