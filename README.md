# Uklana Restaurant App

Native Android Restaurant Partner App for Uklana Food.

Features:
- Admin-approved mobile + PIN login
- One restaurant sees only its WooCommerce category orders
- Restaurant ON/OFF
- Pending and Done orders
- Item quantity, rate, line total and restaurant total
- Done / Food Ready button
- Running total and Reset Total
- Changeable order ringtone
- Local notification for newly detected orders while app is open/background-active

API: https://uklana.food/wp-json/ukf-restaurant/v1

Important: True instant notification after the app is fully force-closed requires a separate Firebase project and server-side FCM sending. This source currently checks for new orders every 15 seconds while the app process is active.
