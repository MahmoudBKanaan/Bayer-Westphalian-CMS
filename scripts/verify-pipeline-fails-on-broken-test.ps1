param(
    [string]$FrontendDir = (Join-Path $PSScriptRoot "..\frontend")
)

$ErrorActionPreference = "Stop"

# Item 706: Pipeline fails on intentionally broken test. Expected result is non-zero.
# Script: verify-pipeline-fails-on-broken-test.ps1
$frontendPath = (Resolve-Path -LiteralPath $FrontendDir).Path
$brokenTest = Join-Path $frontendPath "src\__pipeline_broken__.test.ts"
$relativeTest = "src/__pipeline_broken__.test.ts"
$npmCommand = "npm.cmd"

if (Test-Path -LiteralPath $brokenTest) {
    throw "Refusing to overwrite existing probe test: $brokenTest"
}

try {
    @'
import { describe, expect, it } from "vitest";

describe("intentional CI failure probe", () => {
  it("fails intentionally so CI fail-on-red can be verified", () => {
    expect("pipeline").toBe("red");
  });
});
'@ | Set-Content -LiteralPath $brokenTest -Encoding UTF8

    Push-Location -LiteralPath $frontendPath
    try {
        & $npmCommand test -- --reporter=dot --silent=true $relativeTest
        $testExitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }

    if ($testExitCode -eq 0) {
        throw "Expected the intentionally broken test to fail, but npm test exited 0."
    }

    Write-Host "Intentional broken test failed as expected (exit code $testExitCode)."
    exit 0
}
finally {
    if (Test-Path -LiteralPath $brokenTest) {
        Remove-Item -LiteralPath $brokenTest -Force
    }
}
