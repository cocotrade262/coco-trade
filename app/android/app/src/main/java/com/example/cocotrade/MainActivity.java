package com.example.cocotrade;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends BridgeActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Explicitly allow screenshots by clearing FLAG_SECURE
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);

    // Register custom plugin
    registerPlugin(NativeAuthPlugin.class);
  }

  @CapacitorPlugin(name = "NativeAuth")
  public static class NativeAuthPlugin extends Plugin {

    @PluginMethod
    public void login(PluginCall call) {
      Intent intent = new Intent(getContext(), LoginActivity.class);
      // Clear stack to ensure clean transition
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      getContext().startActivity(intent);
      call.resolve();
    }

    @PluginMethod
    public void getCurrentUser(PluginCall call) {
      FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
      if (user != null) {
        JSObject ret = new JSObject();
        ret.put("uid", user.getUid());
        ret.put("email", user.getEmail());
        ret.put("displayName", user.getDisplayName());
        ret.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);

        // Also try to get a fresh ID token for the JS side to use
        user.getIdToken(false).addOnCompleteListener(task -> {
           if (task.isSuccessful()) {
             ret.put("idToken", task.getResult().getToken());
             call.resolve(ret);
           } else {
             call.resolve(ret); // Resolve without token if failed
           }
        });
      } else {
        call.reject("No user logged in");
      }
    }

    @PluginMethod
    public void logout(PluginCall call) {
      FirebaseAuth.getInstance().signOut();
      call.resolve();
    }
  }
}
