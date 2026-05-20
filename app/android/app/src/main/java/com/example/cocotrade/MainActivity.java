package com.example.cocotrade;

import android.os.Bundle;
import android.view.WindowManager;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Explicitly allow screenshots by clearing FLAG_SECURE
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
  }
}
