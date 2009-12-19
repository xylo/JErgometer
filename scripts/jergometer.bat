goto START

:UPDATE
echo "UPDATE"
move tmp\* .
rmdir tmp

:START
java -Djava.library.path=lib/dlls/Windows/i368 -cp jergometer.jar;RXTXcomm.jar org.jergometer.Jergometer %1

IF errorlevel 255 goto UPDATE
