package com.uklanafood.delivery;
public final class AppConfig {
  private AppConfig(){}
  public static final String API_BASE="https://uklana.food/wp-json/ukf-delivery/v1";
  public static final String LOGIN=API_BASE+"/login", DASHBOARD=API_BASE+"/dashboard", ORDERS=API_BASE+"/orders", ACCEPT=API_BASE+"/accept", FORWARD=API_BASE+"/forward", DELIVERED=API_BASE+"/delivered", UPDATE_STATUS=API_BASE+"/update-status", FCM=API_BASE+"/register-fcm-token";
  public static final long REFRESH_MS=12000L;
}
