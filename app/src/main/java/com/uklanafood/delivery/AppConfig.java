package com.uklanafood.delivery;
public final class AppConfig {
  private AppConfig(){}
  public static final String API_BASE="https://uklana.food/wp-json/ukf-delivery/v1";
  public static final String LOGIN=API_BASE+"/login", DASHBOARD=API_BASE+"/dashboard", ORDERS=API_BASE+"/orders", ACCEPT=API_BASE+"/accept", FORWARD=API_BASE+"/forward", DELIVERED=API_BASE+"/delivered", RESET=API_BASE+"/reset-total", RESET_ONLINE_DELIVERY=API_BASE+"/reset-online-delivery", FCM=API_BASE+"/register-fcm-token";
  public static final long REFRESH_MS=12000L;
}
