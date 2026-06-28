package com.example.cocotrade;

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
                    Log.e(TAG, "CRITICAL: Cloudinary API Secret is invalid or missing. Deletion will fail.");
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
            }
        });
    }

    private static boolean tryDelete(Context context, Cloudinary cloudinary, String publicId, String resourceType) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("resource_type", resourceType);
            options.put("invalidate", true);

            Map result = cloudinary.uploader().destroy(publicId, options);
            Log.d(TAG, resourceType + " deletion attempt for " + publicId + ": " + (result != null ? result.toString() : "null"));

            boolean success = result != null && "ok".equals(result.get("result"));
            if (success) {
                showToast(context, "Cloudinary " + resourceType + " deleted successfully");
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
            } catch (Exception ignored) {}
        });
    }

    private static String extractPublicId(String url) {
        try {
            if (!url.contains("/upload/")) return null;

            // 1. Get the path after /upload/
            String path = url.substring(url.indexOf("/upload/") + 8);

            // 2. Split by / to handle transformations, version, and folders
            String[] parts = path.split("/");

            // 3. Find where the actual public ID starts.
            // We skip transformations (segments with '=' or ',') and the version (vXXXXX)
            int startIndex = 0;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                // Transformations in Cloudinary often contain: , _ . = (e.g. w_400,c_fill or f_auto)
                // However, folders and IDs can also have _ or .
                // The most reliable way to skip metadata is to look for the version string 'v' + digits.
                if (part.startsWith("v") && part.length() > 1 && Character.isDigit(part.charAt(1))) {
                    startIndex = i + 1;
                    break;
                }

                // If we haven't found a version string yet, check if this segment looks like a transformation.
                // Cloudinary transformations usually have specific flags or =.
                if (part.contains(",") || part.contains("=") || part.matches("^[a-z]_[a-z0-9]+.*$")) {
                    continue;
                }

                // If it's not a transformation and we haven't seen 'v' yet, it might be the start of folders.
                startIndex = i;
                break;
            }

            // 4. Join the remaining parts to get the full public ID (including folders)
            StringBuilder publicIdBuilder = new StringBuilder();
            for (int i = startIndex; i < parts.length; i++) {
                if (publicIdBuilder.length() > 0) publicIdBuilder.append("/");
                publicIdBuilder.append(parts[i]);
            }

            String fullPath = publicIdBuilder.toString();

            // 5. Remove the file extension
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
