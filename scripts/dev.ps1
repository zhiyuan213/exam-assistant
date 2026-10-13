param([ValidateSet('miniapp','preview','backend','build','test')][string]$Action='miniapp')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$portableNode=Join-Path $projectRoot '.tools/node-v22.16.0-win-x64'
if(Test-Path -LiteralPath $portableNode){$env:PATH=$portableNode+';'+$env:PATH}
$portableJava=Join-Path $projectRoot '.tools/java/jdk-17.0.20.1+1'
if(Test-Path -LiteralPath $portableJava){$env:JAVA_HOME=$portableJava;$env:PATH=$portableJava+'\bin;'+$env:PATH}
$portableMaven=Join-Path $projectRoot '.tools/apache-maven-3.9.9/bin/mvn.cmd'
$maven=if(Test-Path -LiteralPath $portableMaven){$portableMaven}else{'mvn.cmd'}
function Run-Npm([string[]]$Arguments){& npm.cmd @Arguments;if($LASTEXITCODE -ne 0){throw 'npm command failed'}}
if($Action -eq 'backend'){
    $javaTemp=Join-Path $projectRoot '.tools/java-temp'
    New-Item -ItemType Directory -Force -Path $javaTemp|Out-Null
    Push-Location (Join-Path $projectRoot 'backend')
    try{& $maven spring-boot:run "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=$javaTemp";if($LASTEXITCODE -ne 0){throw 'Backend failed'}}finally{Pop-Location}
    exit
}
Push-Location (Join-Path $projectRoot 'frontend')
try{
    if(!(Test-Path -LiteralPath 'node_modules')){Run-Npm -Arguments @('ci')}
    switch($Action){
        'miniapp'{Run-Npm -Arguments @('run','dev:mp-weixin')}
        'preview'{Run-Npm -Arguments @('run','dev:h5')}
        'build'{Run-Npm -Arguments @('run','build:mp-weixin');Run-Npm -Arguments @('run','build:h5')}
        'test'{Run-Npm -Arguments @('test')}
    }
}finally{Pop-Location}
if($Action -eq 'test'){& $maven -f (Join-Path $projectRoot 'backend/pom.xml') verify;if($LASTEXITCODE -ne 0){throw 'Backend tests failed'}}

