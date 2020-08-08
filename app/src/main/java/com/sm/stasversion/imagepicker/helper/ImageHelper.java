package com.sm.stasversion.imagepicker.helper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.sm.stasversion.imagepicker.model.Asset;
import com.sm.stasversion.imagepicker.model.Image;
import com.sm.stasversion.imagepicker.model.SavePath;
import com.sm.stasversion.imagepicker.model.Video;
import com.sm.stasversion.imagepicker.util.Extensions_FileKt;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by hoanglam on 7/31/16.
 */
public class ImageHelper {

    private static final String TAG = "ImageHelper";

    public static File createAssetFile(boolean isVideoFile, SavePath savePath) {
        // External sdcard location
        final String path = savePath.getPath();
        File mediaStorageDir = savePath.isFullPath()
                ? new File(path)
                : new File(isVideoFile ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), path);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create " + path);
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = (isVideoFile ? "VIDEO_" : "IMG_") + timeStamp;

        File file = null;
        try {
            file = File.createTempFile(fileName, isVideoFile ? ".mp4" : ".jpg", mediaStorageDir);
        } catch (IOException e) {
            Log.d(TAG, "Oops! Failed create " + fileName + " file");
        }
        return file;
    }

    public static String getNameFromFilePath(String path) {
        if (path.contains(File.separator)) {
            return path.substring(path.lastIndexOf(File.separator) + 1);
        }
        return path;
    }

    public static void grantAppPermission(Context context, Intent intent, Uri fileUri) {
        List<ResolveInfo> resolvedIntentActivities = context.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
            String packageName = resolvedIntentInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, fileUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    public static void revokeAppPermission(Context context, Uri fileUri) {
        context.revokeUriPermission(fileUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    public static List<Asset> singleListFromPath(String path, Context context) {
        List<Asset> assets = new ArrayList<>();
        File f = new File(path);
        if (Extensions_FileKt.isImageFile(f)) {
            assets.add(new Image(0, getNameFromFilePath(path), path, "", "", 0));
        } else if (Extensions_FileKt.isVideoFile(f)) {
            assets.add(new Video(0, getNameFromFilePath(path), path, "", "", 0));
        }
        return assets;
    }

    public static boolean isGifFormat(Image image) {
        String extension = image.getPath().substring(image.getPath().lastIndexOf(".") + 1, image.getPath().length());
        return extension.equalsIgnoreCase("gif");
    }


}
