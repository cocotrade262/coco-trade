package com.asn.cocotrade;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import com.cloudinary.Cloudinary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class CloudinaryHelper {
    private static final String TAG = "CloudinaryHelper";

    public static void deleteAsset(Context context, String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isEmpty()) return;

        final String publicId = idOrUrl.startsWith("http") ? extractPublicId(idOrUrl) : idOrUrl;

        if (publicId == null) {
            Log.e(TAG, "Failed to determine publicId for: " + idOrUrl);
            return;
        }

        Log.d(TAG, "Initiating Cloudinary asset deletion: " + publicId);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME;
                String apiKey = BuildConfig.CLOUDINARY_API_KEY;
                String apiSecret = BuildConfig.CLOUDINARY_API_SECRET;

                if (apiSecret == null || apiSecret.isEmpty() || apiSecret.equals("your_secret_here")) {
                    Log.w(TAG, "Cloudinary API Secret is missing or empty in BuildConfig. Using default fallback secret.");
                    apiSecret = "gxOw2EhEoLi9FUBQcGExoiYJfgM";
                }

                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", cloudName);
                config.put("api_key", apiKey);
                config.put("api_secret", apiSecret);

                Cloudinary cloudinary = new Cloudinary(config);

                // Attempt 1: Video resource type (Most likely)
                if (tryDelete(context, cloudinary, publicId, "video")) return;

                // Attempt 2: Image resource type (Fallback for thumbnails or misclassified)
                if (tryDelete(context, cloudinary, publicId, "image")) return;

                // Attempt 3: Raw resource type (Fallback for non-standard uploads)
                tryDelete(context, cloudinary, publicId, "raw");

            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during deletion of " + publicId, e);
                showToast(context, "Deletion failed: " + e.getMessage());
            }
        });
    }

    private static boolean tryDelete(Context context, Cloudinary cloudinary, String publicId, String resourceType) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("resource_type", resourceType);
            options.put("invalidate", true);

            Map result = cloudinary.uploader().destroy(publicId, options);
            String resultStr = (result != null ? result.toString() : "null");
            Log.d(TAG, resourceType + " deletion attempt for " + publicId + ": " + resultStr);

            boolean success = result != null && "ok".equals(result.get("result"));
            if (success) {
                showToast(context, "Asset deleted from Cloudinary");
            } else if (result != null && result.containsKey("error")) {
                Map error = (Map) result.get("error");
                String errorMsg = error != null ? (String) error.get("message") : "Unknown Cloudinary error";
                // If not found, we don't necessarily want to toast every fallback failure
                if (!"not found".equalsIgnoreCase(errorMsg)) {
                    showToast(context, "Cloudinary error (" + resourceType + "): " + errorMsg);
                }
            }
            return success;
        } catch (Exception e) {
            Log.w(TAG, resourceType + " deletion attempt failed for " + publicId + ". Error: " + e.getMessage());
            return false;
        }
    }

    private static void showToast(Context context, String message) {
        if (context == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing toast: " + message, e);
            }
        });
    }

    private static String extractPublicId(String url) {
        try {
            if (!url.contains("/upload/")) return null;

            String path = url.substring(url.indexOf("/upload/") + 8);
            String[] parts = path.split("/");

            int startIndex = 0;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                // Transformations contain , or =
                if (part.contains(",") || part.contains("=")) continue;
                // Version starts with v + digits
                if (part.startsWith("v") && part.length() > 1 && Character.isDigit(part.charAt(1))) {
                    startIndex = i + 1;
                    break;
                }
                // Known transformation patterns without =
                if (part.startsWith("f_") || part.startsWith("q_") || part.startsWith("w_") || part.startsWith("vc_")) {
                    continue;
                }
                // Start of identifiers/folders
                startIndex = i;
                break;
            }

            StringBuilder publicIdBuilder = new StringBuilder();
            for (int i = startIndex; i < parts.length; i++) {
                if (publicIdBuilder.length() > 0) publicIdBuilder.append("/");
                publicIdBuilder.append(parts[i]);
            }

            String fullPath = publicIdBuilder.toString();
            int lastDot = fullPath.lastIndexOf('.');
            if (lastDot != -1) {
                return fullPath.substring(0, lastDot);
            }
            return fullPath;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting publicId from URL: " + url, e);
            return null;
        }
    }
}
