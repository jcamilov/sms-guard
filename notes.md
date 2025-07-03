
## debugging
Send SMSs via terminal
cd C:\Users\la_DevAdmin3\AppData\Local\Android\Sdk\platform-tools\ 

# App install (from terminal AS)
If necessary:
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
then
./gradlew installDebug  


# test app
Test with [test_sms.bat](test_sms.bat) by running
```bash
./test_sms.bat
```