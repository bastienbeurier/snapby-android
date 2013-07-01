package com.streetshout.android.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import com.streetshout.android.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {
    public static boolean isSDPresent() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static Uri reserveUriForPicture(Context ctx) {
        String fileName = "streetshout.jpg";	// testing
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, "Street Shout");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri photoUri = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        return photoUri;
    }

    public static Intent getPhotoChooserIntent(Context ctx, Uri photoUri) {
        List<Intent> cameraIntents = new ArrayList<Intent>();

        Intent imageCaptureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager packageManager = ctx.getPackageManager();
        List<ResolveInfo> listCameras = packageManager.queryIntentActivities(imageCaptureIntent, 0);

        for (ResolveInfo camera : listCameras) {
            String packageName = camera.activityInfo.packageName;
            Intent cameraIntent = new Intent(imageCaptureIntent);
            cameraIntent.setComponent(new ComponentName(camera.activityInfo.packageName, camera.activityInfo.name));
            cameraIntent.setPackage(packageName);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraIntents.add(cameraIntent);
        }

        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        Intent chooserIntent = Intent.createChooser(galleryIntent, ctx.getString(R.string.select_picture_from));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

        return chooserIntent;
    }

    public static Uri getImageUrl(Activity activity, Intent data, Uri reserverdUri) {
        Uri	imagePath	= null;

        if (data == null || data.getData() == null) {
            imagePath	= reserverdUri;
        } else {
            imagePath	= data.getData();
        }

        return imagePath;
    }

    static public Bitmap shrinkBitmap(String file, int width, int height){

        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);

        int heightRatio = (int)Math.ceil(bmpFactoryOptions.outHeight/(float)height);
        int widthRatio = (int)Math.ceil(bmpFactoryOptions.outWidth/(float)width);

        if (heightRatio > 1 || widthRatio > 1) {
            if (heightRatio > widthRatio) {
                bmpFactoryOptions.inSampleSize = heightRatio;
            } else {
                bmpFactoryOptions.inSampleSize = widthRatio;
            }
        }

        bmpFactoryOptions.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);
        return bitmap;
    }

    static public void storeBitmapInFile(String pathName, Bitmap bm) {
        File file = new File(pathName);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
