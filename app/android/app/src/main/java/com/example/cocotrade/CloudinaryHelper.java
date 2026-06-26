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
            Log.d(TAG, resourceType + " deletion attempt for " + publicId + ": " + (result != null ? result.toString() : "null"));

            return result != null && "ok".equals(result.get("result"));
        } catch (Exception e) {
            Log.w(TAG, resourceType + " deletion attempt failed for " + publicId + ". Error: " + e.getMessage());
            return false;
        }
    }

    private static String extractPublicId(String url) {
        try {
            if (!url.contains("/upload/")) return null;

            // Remove the part before /upload/ and the /upload/ itself
            String path = url.substring(url.indexOf("/upload/") + 8);

            // The path now looks like: [transformations/]v12345678/optional_folder/public_id.mp4
            String[] parts = path.split("/");

            int startIndex = 0;
            for (int i = 0; i < parts.length; i++) {
                String segment = parts[i];

                // Skip transformation segments (contain comma or equal sign)
                if (segment.contains(",") || segment.contains("=")) {
                    continue;
                }

                // Skip version segment (starts with 'v' followed by digits)
                if (segment.startsWith("v") && segment.length() > 1 && Character.isDigit(segment.charAt(1))) {
                    startIndex = i + 1;
                    break;
                }

                // If we encounter a segment that doesn't look like a version or transformation,
                // it might be the start of the public ID in a URL without a version string.
                // But Cloudinary URLs almost always have a version.
                // Let's assume the first non-transformation segment that isn't 'v' is the ID.
                startIndex = i;
                break;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = startIndex; i < parts.length; i++) {
                if (sb.length() > 0) sb.append("/");
                sb.append(parts[i]);
            }

            String fullIdWithExtension = sb.toString();
            int lastDot = fullIdWithExtension.lastIndexOf('.');
            if (lastDot != -1) {
                return fullIdWithExtension.substring(0, lastDot);
            }
            return fullIdWithExtension;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting publicId from URL: " + url, e);
            return null;
        }
    }
}
