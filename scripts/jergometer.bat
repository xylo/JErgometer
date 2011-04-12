goto START

:UPDATE
echo "UPDATE"
xcopy /E /Y update .
rmdir /S /Q update

:START
start\windows_start.bat

IF errorlevel 255 goto UPDATE
goto END


:END
