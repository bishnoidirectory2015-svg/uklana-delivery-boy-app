package com.uklanafood.delivery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private LinearLayout ordersContainer;
    private TextView runningTotal, onlineDue, statusText, deliveryName, emptyText;
    private Button pendingTab, doneTab;
    private String currentType = "pending";
    private JSONArray companyHistory = new JSONArray();
    private JSONArray onlineChargeHistory = new JSONArray();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override public void run() { load(); handler.postDelayed(this, AppConfig.REFRESH_MS); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.loggedIn(this)) { startActivity(new Intent(this, LoginActivity.class)); finish(); return; }
        setContentView(R.layout.activity_main);
        bindViews();
        deliveryName.setText(SessionManager.name(this) + "  •  Priority " + SessionManager.priority(this));
        pendingTab.setOnClickListener(v -> switchTab("pending"));
        doneTab.setOnClickListener(v -> switchTab("done"));
        findViewById(R.id.resetTotalButton).setOnClickListener(v -> resetCompanyCash());
        findViewById(R.id.resetOnlineDeliveryButton).setOnClickListener(v -> resetOnlineDelivery());
        findViewById(R.id.companyHistoryButton).setOnClickListener(v -> showHistory("Company Cash History", companyHistory));
        findViewById(R.id.onlineHistoryButton).setOnClickListener(v -> showHistory("Online Delivery Charge History", onlineChargeHistory));
        findViewById(R.id.refreshButton).setOnClickListener(v -> load());
        findViewById(R.id.menuButton).setOnClickListener(this::showMenu);
        NotificationHelper.channel(this);
        if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9);
        }
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> new Thread(() -> {
            try { ApiClient.post(this, AppConfig.FCM, new JSONObject().put("fcm_token", token)); } catch (Exception ignored) {}
        }).start());
        switchTab("pending");
    }

    private void bindViews() {
        ordersContainer = findViewById(R.id.ordersContainer);
        runningTotal = findViewById(R.id.runningTotal);
        onlineDue = findViewById(R.id.onlineDeliveryDue);
        statusText = findViewById(R.id.statusText);
        deliveryName = findViewById(R.id.deliveryName);
        pendingTab = findViewById(R.id.pendingTab);
        doneTab = findViewById(R.id.doneTab);
        emptyText = findViewById(R.id.emptyText);
    }

    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(poll); handler.postDelayed(poll, 12000); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(poll); }

    private void switchTab(String type) {
        currentType = type;
        pendingTab.setSelected("pending".equals(type));
        doneTab.setSelected("done".equals(type));
        pendingTab.setTextColor("pending".equals(type) ? Color.WHITE : 0xff3F4854);
        doneTab.setTextColor("done".equals(type) ? Color.WHITE : 0xff3F4854);
        load();
    }


    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Change Order Ringtone");
        menu.getMenu().add("Refresh Orders");
        menu.getMenu().add("Logout");
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            if (title.startsWith("Change")) {
                startActivity(new Intent(this, RingtoneSettingsActivity.class));
                return true;
            }
            if (title.startsWith("Refresh")) { load(); return true; }
            if (title.equals("Logout")) {
                new AlertDialog.Builder(this).setTitle("Logout?")
                    .setMessage("Delivery Boy account se logout karna hai?")
                    .setPositiveButton("LOGOUT", (d,w) -> { SessionManager.clear(this); recreate(); })
                    .setNegativeButton("CANCEL", null).show();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void load() {
        statusText.setText("Refreshing…");
        new Thread(() -> {
            try {
                JSONObject dashboard = ApiClient.get(this, AppConfig.DASHBOARD);
                JSONObject orderResponse = ApiClient.get(this, AppConfig.ORDERS + "?type=" + currentType);
                JSONArray orders = orderResponse.optJSONArray("orders");
                runOnUiThread(() -> render(dashboard, orders));
            } catch (Exception e) {
                runOnUiThread(() -> statusText.setText("Connection error: " + e.getMessage()));
            }
        }).start();
    }

    private void render(JSONObject dashboard, JSONArray orders) {
        runningTotal.setText(dashboard.optString("running_total", "₹0.00"));
        onlineDue.setText(dashboard.optString("online_delivery_due", "₹0.00"));
        statusText.setText("Pending: " + dashboard.optInt("assigned_count") + "   •   Delivered: " + dashboard.optInt("delivered_count"));
        ordersContainer.removeAllViews();
        int visibleCount = 0;
        int count = orders == null ? 0 : orders.length();
        for (int i = 0; i < count; i++) {
            JSONObject order = orders.optJSONObject(i);
            if (order == null) continue;

            boolean delivered = isDeliveredOrder(order);
            if ("pending".equals(currentType) && delivered) continue;
            if ("pending".equals(currentType) && isLegacyStalePending(order)) continue;
            if ("done".equals(currentType) && !delivered) continue;

            addOrderCard(order);
            visibleCount++;
        }
        emptyText.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
        emptyText.setText("done".equals(currentType) ? "No completed orders yet" : "No pending delivery orders");
        companyHistory = dashboard.optJSONArray("reset_history");
        if (companyHistory == null) companyHistory = new JSONArray();
        onlineChargeHistory = dashboard.optJSONArray("online_delivery_reset_history");
        if (onlineChargeHistory == null) onlineChargeHistory = new JSONArray();
    }

    private void addOrderCard(JSONObject o) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.order_card_pro);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(14));

        boolean deliveredOrder = isDeliveredOrder(o);
        String statusLabel = cleanStatusLabel(o);
        TextView title = text("Order #" + o.optString("number") + "  •  " + statusLabel.toUpperCase(), 20, true);
        title.setTextColor(getResources().getColor(deliveredOrder ? android.R.color.holo_green_dark : android.R.color.holo_orange_dark));
        card.addView(title);
        addLine(card, "Restaurant", o.optString("restaurant"));
        addLine(card, "Order Time", o.optString("order_date"));
        addSection(card, "CUSTOMER DETAILS");
        addLine(card, "Name", o.optString("customer_name"));
        addLine(card, "Phone", o.optString("phone"));
        addLine(card, "Address", o.optString("address"));
        addLine(card, "Nearby", o.optString("nearby"));
        addLine(card, "Distance", withKm(o.optString("distance")));
        addSection(card, "ORDER SUMMARY");
        TextView items = text(o.optString("items", "No item details"), 16, false);
        items.setPadding(0, dp(5), 0, dp(8));
        card.addView(items);
        addLine(card, "Food Amount", o.optString("food_amount"));
        addLine(card, "Delivery Charge", o.optString("delivery_charge"));
        addLine(card, "Customer Total", o.optString("customer_total"));
        addLine(card, "Payment", o.optString("payment_status"));
        addLine(card, "Company Cash", o.optString("collect_amount"));
        String note = o.optString("customer_note");
        if (!note.isEmpty()) addLine(card, "Customer Note", note);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        Button call = actionButton("CALL");
        Button map = actionButton("MAP / LOCATION");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(48), 1);
        bp.setMargins(0, dp(8), dp(5), 0);
        actions.addView(call, bp);
        LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(0, dp(48), 1);
        bp2.setMargins(dp(5), dp(8), 0, 0);
        actions.addView(map, bp2);
        call.setOnClickListener(v -> openDial(o.optString("call_phone", o.optString("phone"))));
        map.setOnClickListener(v -> openMap(o.optString("map_link"), o.optString("address")));
        card.addView(actions);

        if (!deliveredOrder) {
            String nextStatus = resolveNextStatus(o);
            String nextLabel = resolveNextLabel(o, nextStatus);
            Button statusButton = actionButton(nextLabel.toUpperCase());
            statusButton.setTextSize(15);
            LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, this.dp(50));
            statusLp.setMargins(0, this.dp(10), 0, 0);
            card.addView(statusButton, statusLp);
            statusButton.setEnabled(!nextStatus.isEmpty());
            statusButton.setOnClickListener(v -> updateOrderStatus(o.optString("id"), nextStatus, nextLabel));
        } else {
            addLine(card, "Delivered At", o.optString("delivered_at"));
        }
        ordersContainer.addView(card, cp);
    }



    /**
     * Older API/plugin versions left already-finished orders in the pending feed with
     * only a generic PENDING label and no actionable next status. New valid orders
     * always include a current WooCommerce status or a next_status value. Hide only
     * those non-actionable legacy rows so genuine new orders remain visible.
     */
    private boolean isLegacyStalePending(JSONObject order) {
        if (isDeliveredOrder(order)) return false;

        String next = order.optString("next_status", "").trim();
        if (!next.isEmpty()) return false;

        String wcStatus = order.optString("wc_status", "").trim().toLowerCase();
        String status = order.optString("status", "").trim().toLowerCase();
        String label = order.optString("status_label", "").trim().toLowerCase();

        if (wcStatus.startsWith("wc-")) wcStatus = wcStatus.substring(3);
        if (status.startsWith("wc-")) status = status.substring(3);

        boolean genericPending =
                wcStatus.isEmpty() &&
                (status.isEmpty() || status.equals("pending") || status.equals("assigned")) &&
                (label.isEmpty() || label.equals("pending") || label.equals("assigned"));

        return genericPending;
    }

    private boolean isDeliveredOrder(JSONObject order) {
        String[] values = {
                order.optString("wc_status", ""),
                order.optString("status", ""),
                order.optString("status_label", ""),
                order.optString("next_status", "")
        };
        for (String value : values) {
            String normalized = value == null ? "" : value.trim().toLowerCase();
            if (normalized.startsWith("wc-")) normalized = normalized.substring(3);
            if (normalized.equals("ukf-delivered") || normalized.equals("delivered") ||
                    normalized.equals("completed") || normalized.equals("complete") ||
                    normalized.contains("delivered") || normalized.contains("completed")) {
                return true;
            }
        }
        return !order.optString("delivered_at", "").trim().isEmpty();
    }

    private String cleanStatusLabel(JSONObject order) {
        if (isDeliveredOrder(order)) return "Delivered";
        String label = order.optString("status_label", "").trim();
        if (!label.isEmpty()) return label;
        String status = order.optString("wc_status", order.optString("status", "pending")).trim();
        if (status.startsWith("wc-")) status = status.substring(3);
        switch (status) {
            case "processing":
            case "ukf-accepted": return "Restaurant Accepted";
            case "ukf-preparing": return "Food Preparing";
            case "ukf-pickup": return "Picked Up";
            case "ukf-out": return "Out for Delivery";
            default: return "Pending";
        }
    }

    private String resolveNextStatus(JSONObject order) {
        String next = order.optString("next_status", "").trim();
        if (!next.isEmpty()) return next;
        String status = order.optString("wc_status", order.optString("status", "")).trim().toLowerCase();
        if (status.startsWith("wc-")) status = status.substring(3);
        switch (status) {
            case "pending":
            case "on-hold":
            case "ukf-received": return "ukf-accepted";
            case "processing":
            case "ukf-accepted":
            case "accepted": return "ukf-preparing";
            case "ukf-preparing":
            case "preparing": return "ukf-pickup";
            case "ukf-pickup":
            case "pickup": return "ukf-out";
            case "ukf-out":
            case "out": return "ukf-delivered";
            default: return "";
        }
    }

    private String resolveNextLabel(JSONObject order, String nextStatus) {
        String label = order.optString("next_label", "").trim();
        if (!label.isEmpty()) return label;
        switch (nextStatus) {
            case "ukf-accepted": return "Restaurant Accepted";
            case "ukf-preparing": return "Food Preparing";
            case "ukf-pickup": return "Picked Up";
            case "ukf-out": return "Out for Delivery";
            case "ukf-delivered": return "Delivered";
            default: return "Refresh Status";
        }
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(0xff222222);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private void addSection(LinearLayout card, String label) { TextView t = text(label, 15, true); t.setPadding(0, dp(13), 0, dp(3)); t.setTextColor(0xffe65100); card.addView(t); }
    private void addLine(LinearLayout card, String label, String value) { if (value == null || value.trim().isEmpty()) return; TextView t = text(label + ": " + value, 15, false); t.setPadding(0, dp(2), 0, dp(2)); card.addView(t); }
    private Button actionButton(String label) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); return b; }
    private String withKm(String value) { if (value == null || value.isEmpty()) return ""; return value.toLowerCase().contains("km") ? value : value + " KM"; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void openDial(String phone) { if (phone == null || phone.isEmpty()) { toast("Phone number not available"); return; } startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))); }
    private void openMap(String link, String address) {
        String target = link == null ? "" : link.trim();
        if (target.isEmpty() && address != null && !address.isEmpty()) target = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(address);
        if (target.isEmpty()) { toast("Customer location not saved"); return; }
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(target))); } catch (Exception e) { toast("Map app not available"); }
    }

    private void showHistory(String title, JSONArray array) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(10), dp(20), dp(10));
        if (array == null || array.length() == 0) {
            TextView empty = text("No history", 15, false);
            empty.setPadding(0, dp(12), 0, dp(12));
            content.addView(empty);
        } else {
            for (int i = 0; i < array.length(); i++) {
                JSONObject x = array.optJSONObject(i);
                TextView row = text((i + 1) + ".  " + x.optString("date_time") + "  —  " + x.optString("amount"), 14, false);
                row.setPadding(0, dp(10), 0, dp(10));
                content.addView(row);
            }
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(content);
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scroll)
            .setPositiveButton("CLOSE", null)
            .show();
    }

    private void updateOrderStatus(String id, String nextStatus, String nextLabel) {
        if (nextStatus == null || nextStatus.isEmpty()) { toast("Next status available nahi hai"); return; }
        String message = "ukf-delivered".equals(nextStatus)
            ? "Customer ko order dene aur payment confirm karne ke baad hi Delivered karein."
            : "Order status ko " + nextLabel + " karna hai?";
        new AlertDialog.Builder(this)
            .setTitle(nextLabel + "?")
            .setMessage(message)
            .setPositiveButton("YES", (d, w) -> new Thread(() -> {
                try {
                    JSONObject body = new JSONObject().put("order_id", id).put("status", nextStatus);
                    JSONObject r = ApiClient.post(this, AppConfig.UPDATE_STATUS, body);
                    runOnUiThread(() -> toast(r.optString("message", "Status updated")));
                    load();
                } catch (Exception e) {
                    runOnUiThread(() -> toast(e.getMessage()));
                }
            }).start())
            .setNegativeButton("CANCEL", null).show();
    }
    private void resetOnlineDelivery() { new AlertDialog.Builder(this).setTitle("Online delivery charge paid?").setMessage("Admin se online delivery charge milne ke baad hi reset karein.").setPositiveButton("RESET", (d,w)->new Thread(()->{try{ApiClient.post(this,AppConfig.RESET_ONLINE_DELIVERY,new JSONObject());load();}catch(Exception e){runOnUiThread(()->toast(e.getMessage()));}}).start()).setNegativeButton("CANCEL",null).show(); }
    private void resetCompanyCash() { new AlertDialog.Builder(this).setTitle("Submit and reset company cash?").setMessage("Company ko cash jama karne ke baad hi reset karein.").setPositiveButton("RESET", (d,w)->new Thread(()->{try{ApiClient.post(this,AppConfig.RESET,new JSONObject());load();}catch(Exception e){runOnUiThread(()->toast(e.getMessage()));}}).start()).setNegativeButton("CANCEL",null).show(); }
    private void toast(String s) { Toast.makeText(this, s == null ? "" : s, Toast.LENGTH_LONG).show(); }
}
