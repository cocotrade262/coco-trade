package com.example.cocotrade;

import android.util.Log;
import com.cloudinary.Cloudinary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class CloudinaryHelper {
    private static final String TAG = "CloudinaryHelper";

    public static void deleteAsset(String idOrUrl) {
        if (idOrUrl == null || idOrUrl.isEmpty()) return;

        final String publicId = idOrUrl.startsWith("http") ? extractPublicId(idOrUrl) : idOrUrl;

        if (publicId == null) {
            Log.e(TAG, "Failed to determine publicId for: " + idOrUrl);
            return;
        }

        Log.d(TAG, "Initiating Cloudinary asset deletion: " + publicId);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String secret = BuildConfig.CLOUDINARY_API_SECRET;
                if (secret == null || secret.isEmpty()) {
                    Log.e(TAG, "CRITICAL: Cloudinary API Secret is missing. Deletion aborted.");
                    return;
                }

                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "dt8dfsjjv");
                config.put("api_key", "127825961938747");
                config.put("api_secret", secret);

                Cloudinary cloudinary = new Cloudinary(config);

                // Attempt 1: Video resource type (Most likely)
                if (tryDelete(cloudinary, publicId, "video")) return;

                // Attempt 2: Image resource type (Fallback for thumbnails or misclassified)
                if (tryDelete(cloudinary, publicId, "image")) return;

                // Attempt 3: Raw resource type (Fallback for non-standard uploads)
                tryDelete(cloudinary, publicId, "raw");

            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during deletion of " + publicId, e);
            }
        });
    }

    private static boolean tryDelete(Cloudinary cloudinary, String publicId, String resourceType) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("resource_type", resourceType);
            options.put("invalidate", true);

            Map result = cloudinary.uploader().destroy(publicId, options);
            Log.d(TAG, resourceType + " deletion attempt for " + publicId + ": " + result.toString());

            return "ok".equals(result.get("result"));
        } catch (Exception e) {
            Log.w(TAG, resourceType + " deletion attempt failed for " + publicId, e);
            return false;
        }
    }

    private static String extractPublicId(String url) {
        try {
            if (!url.contains("/upload/")) return null;

            String path = url.substring(url.indexOf("/upload/") + 8);
            // Example: [transformations/]v12345678/optional_folder/public_id.mp4
            String[] parts = path.split("/");

            int startIndex = 0;
            for (int i = 0; i < parts.length; i++) {
                String segment = parts[i];
                // Skip transformation segments (contain comma or equal sign)
                if (segment.contains(",") || segment.contains("=")) {
                    continue;
                }
                // Version starts with 'v' followed by digits
                if (segment.startsWith("v") && segment.length() > 1 && Character.isDigit(segment.charAt(1))) {
                    startIndex = i + 1;
                    break;
                }
            }

            StringBuilder sb = new StringBuilder();
            for (int i = startIndex; i < parts.length; i++) {
                if (sb.length() > 0) sb.append("/");
                sb.append(parts[i]);
            }

            String fullId = sb.toString();
            int lastDot = fullId.lastIndexOf('.');
            if (lastDot != -1) {
                return fullId.substring(0, lastDot);
            }
            return fullId;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting publicId from URL: " + url, e);
            return null;
        }
    }
}
