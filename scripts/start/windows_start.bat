if "%PROCESSOR_ARCHITECTURE%" == "x86" goto START_X86
goto START_AMD64


:START_X86
set ARCH=i386
goto CONTINUE

:START_AMD64
set ARCH=amd64


:CONTINUE
java -Djava.library.path=lib/dlls/Windows/%ARCH% -cp jergometer.jar;lib\RXTXcomm.jar;lib\velocity-1.7-dep.jar org.jergometer.Jergometer %1
EXIT /B %errorlevel%
