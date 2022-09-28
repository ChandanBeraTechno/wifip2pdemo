package com.techno.bobackwifi;

import android.app.Application;
import android.content.Context;

public class Variables extends Application {
  public static final String APP_TYPE = "com.techno.bobackwifi";
  public static final String NAME_DEVICE = "com.techno.bobackwifi.name";

  private static Variables instance;

  @Override
  public void onCreate() {
    instance = this;
    super.onCreate();
  }

  public static Variables getInstance() {
    return instance;
  }

  public static Context getContext() {
    return instance;
    // or return instance.getApplicationContext();
  }
}
