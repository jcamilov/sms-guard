@echo off
echo Testing SMS Guard App with different SMS scenarios...

echo.
echo 1. Testing benign SMS...
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED -n com.example.smsguard/.receiver.SMSReceiver --es sender "123456789" --es message "Hola, tu pedido ha sido confirmado. Gracias por tu compra."

timeout /t 3 /nobreak >nul

echo.
echo 2. Testing smishing SMS with fake bank message...
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED -n com.example.smsguard/.receiver.SMSReceiver --es sender "BANCO123" --es message "Tu cuenta bancaria ha sido bloqueada. Haz clic aquí para desbloquear: http://bit.ly/fake-bank-link"

timeout /t 3 /nobreak >nul

echo.
echo 3. Testing smishing SMS with fake prize...
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED -n com.example.smsguard/.receiver.SMSReceiver --es sender "PROMO" --es message "¡Gana un iPhone gratis! Visita: http://suspicious-link.com/iphone"

timeout /t 3 /nobreak >nul

echo.
echo 4. Testing smishing SMS with urgent action required...
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED -n com.example.smsguard/.receiver.SMSReceiver --es sender "URGENTE" --es message "Tu paquete no pudo ser entregado. Confirma tu dirección: http://fake-delivery.com"

echo.
echo SMS testing completed!
pause 