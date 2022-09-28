package com.techno.bobackwifi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

  public int numbPermissions = 9;
  public String[] permissions = new String[numbPermissions];

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    permissions[0] = android.Manifest.permission.ACCESS_NETWORK_STATE;
    permissions[1] = android.Manifest.permission.ACCESS_WIFI_STATE;
    permissions[2] = android.Manifest.permission.CHANGE_WIFI_STATE;
    permissions[3] = android.Manifest.permission.INTERNET;
    permissions[4] = android.Manifest.permission.READ_EXTERNAL_STORAGE;
    permissions[5] = android.Manifest.permission.CHANGE_NETWORK_STATE;
    permissions[6] = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
    permissions[7] = Manifest.permission.ACCESS_COARSE_LOCATION;
    permissions[8] = Manifest.permission.ACCESS_FINE_LOCATION;

    if (!checkPermissions()) {
      ActivityCompat.requestPermissions(MainActivity.this, permissions, 49);
    } else {
      Intent intent = new Intent(MainActivity.this, FileActivity.class);
//      intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
      this.startActivity(intent);
    }

  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String permissions[], @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case 49: {
        if (!checkPermissions()) {
          ActivityCompat.requestPermissions(MainActivity.this, permissions, 49);
        } else {
          Intent intent = new Intent(MainActivity.this, FileActivity.class);
//          intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
          this.startActivity(intent);
        }
      }
    }
  }

  public boolean checkPermissions() {
    for (int i = 0; i < numbPermissions; i++) {
      if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED)
        return false;
    }
    return true;
  }
}
