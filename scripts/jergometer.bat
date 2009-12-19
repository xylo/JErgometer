goto START

:UPDATE
echo "UPDATE"
move tmp\* .
rmdir tmp

:START
java -jar JLatexEditor.jar %1

IF errorlevel 255 goto UPDATE
