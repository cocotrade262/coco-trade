package com.example.cocotrade;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class CocoTradeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dt8dfsjjv");
        config.put("api_key", "127825961938747");
        config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
        MediaManager.init(this, config);
    }
}
