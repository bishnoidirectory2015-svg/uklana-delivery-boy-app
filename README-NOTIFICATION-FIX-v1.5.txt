Uklana Restaurant App v1.5

Background notification fixes:
- App and Firebase server now use the same Android notification channel: uklana_restaurant_orders_v1
- Default Firebase notification channel added to AndroidManifest.xml
- High importance channel, vibration, lock-screen visibility and ringtone supported
- Firebase service remains available when app is swiped away or screen is off
- FCM token is uploaded after login and whenever Firebase refreshes it

Important Android rule: If the user force-stops the app from Phone Settings, Android blocks all notifications until the app is opened again.
