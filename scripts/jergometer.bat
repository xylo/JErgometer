goto START

:UPDATE
echo "UPDATE"
xcopy /E /Y tmp .
rmdir /S /Q tmp

:START
if "%PROCESSOR_ARCHITECTURE%" == "x86" goto START_X86
goto START_AMD64


:START_X86
java -Djava.library.path=lib/dlls/Windows/i368 -cp jergometer.jar;lib\RXTXcomm.jar org.jergometer.Jergometer %1

IF errorlevel 255 goto UPDATE
goto END


:START_AMD64
java -Djava.library.path=lib/dlls/Windows/i368 -cp jergometer.jar;lib\RXTXcomm.jar org.jergometer.Jergometer %1

IF errorlevel 255 goto UPDATE
goto END


:END
