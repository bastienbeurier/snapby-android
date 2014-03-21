package com.streetshout.android.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import com.streetshout.android.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {

    public static boolean isSDPresent() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    static public Bitmap getResizedBitmap(Bitmap bm, int resolution) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        if (height > resolution && width > resolution) {

            float scale = ((float) resolution) / Math.min(width, height);
            // CREATE A MATRIX FOR THE MANIPULATION
            Matrix matrix = new Matrix();
            // RESIZE THE BIT MAP
            matrix.postScale(scale, scale);

            // "RECREATE" THE NEW BITMAP
            Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
            return resizedBitmap;
        } else {
            return bm;
        }
    }

    static public Bitmap decodeFileAndShrinkBitmap(String filePath, int resolution) {
        Bitmap bitmap = null;

        //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        try {
            FileInputStream fis = new FileInputStream(filePath);
            BitmapFactory.decodeStream(fis, null, o);

            fis.close();
            int ratio = 1;
            if (o.outHeight > resolution && o.outWidth > resolution) {
                float scale = ((float) resolution) / Math.min(o.outHeight, o.outWidth);
                ratio = (int) (1 / scale);
            }

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = ratio;
            fis = new FileInputStream(filePath);
            bitmap = BitmapFactory.decodeStream(fis, null, o2);
            fis.close();

            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }

    static public Bitmap decodeAndMakeThumb(String filePath) {
        Bitmap bitmap = decodeFileAndShrinkBitmap(filePath, Constants.SHOUT_THUMB_RES);
        bitmap = makeSquareBitmap(bitmap);

        return bitmap;
    }

    static public Bitmap makeThumb(Bitmap bitmap) {
        bitmap = getResizedBitmap(bitmap, Constants.SHOUT_THUMB_RES);
        bitmap = makeSquareBitmap(bitmap);

        return bitmap;
    }

    static public Bitmap mirrorBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }


    static public Bitmap makeSquareBitmap(Bitmap bitmap) {
        if (bitmap.getWidth() >= bitmap.getHeight()){

            return Bitmap.createBitmap(
                    bitmap,
                    bitmap.getWidth()/2 - bitmap.getHeight()/2,
                    0,
                    bitmap.getHeight(),
                    bitmap.getHeight()
            );

        }else{

            return Bitmap.createBitmap(
                    bitmap,
                    0,
                    bitmap.getHeight()/2 - bitmap.getWidth()/2,
                    bitmap.getWidth(),
                    bitmap.getWidth()
            );
        }
    }

    static public String getPathFromUri(Context ctx, Uri selectedImage) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        Cursor cursor = ctx.getContentResolver().query(selectedImage,
                filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String photoPath = cursor.getString(columnIndex);
        cursor.close();

        return photoPath;
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

    static public Bitmap rotateImage(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    static public Bitmap reverseRotateImage(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(270);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y - getNavigationBarHeight(context);
    }

    public static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static void setBackground(Context context, View view, int drawable) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(context.getResources().getDrawable(drawable));
        } else {
            view.setBackground(context.getResources().getDrawable(drawable));
        }
    }

    public static Intent getPhotoChooserIntent(Context ctx) {
        List<Intent> cameraIntents = new ArrayList<Intent>();

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntents.add(cameraIntent);

        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        Intent chooserIntent = Intent.createChooser(galleryIntent, ctx.getString(R.string.select_picture_from));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

        return chooserIntent;
    }
}
