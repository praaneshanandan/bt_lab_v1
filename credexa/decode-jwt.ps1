# Decode JWT Token

# Login as customer1
$loginBody = @{
    usernameOrEmailOrMobile = "customer1"
    password = "Pass1234"
} | ConvertTo-Json

$loginResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -UseBasicParsing
$loginData = $loginResponse.Content | ConvertFrom-Json
$token = $loginData.data.token

Write-Host "Token: $token`n" -ForegroundColor Cyan

# Decode JWT (split by dots and decode base64)
$parts = $token.Split('.')
$header = $parts[0]
$payload = $parts[1]

# Add padding if needed
while ($payload.Length % 4 -ne 0) {
    $payload += "="
}

# Convert from base64
$payloadBytes = [System.Convert]::FromBase64String($payload)
$payloadJson = [System.Text.Encoding]::UTF8.GetString($payloadBytes)

Write-Host "Decoded Payload:" -ForegroundColor Yellow
$payloadJson | ConvertFrom-Json | ConvertTo-Json -Depth 10

Write-Host "`nRoles in token:" -ForegroundColor Green
($payloadJson | ConvertFrom-Json).roles
