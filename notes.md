
## debugging

### Copy the into the emulator:
./adb shell mkdir -p /data/local/tmp/llm/
./adb push "C:\Users\la_DevAdmin3\OneDrive - Fraunhofer\Desktop\Gemma_3n_E2B_it_int4\20250520\gemma-3n-E2B-it-int4.task" "/data/local/tmp/llm/gemma-3n-E2B-it-int4.task"

### Send SMSs via terminal
cd C:\Users\la_DevAdmin3\AppData\Local\Android\Sdk\platform-tools\ 
./adb emu sms send 111 "Hey, what are u up to?"
./adb emu sms send 222 "You have tax return unclaimed. Click for more info https://tax-return.ru/unpaid"


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