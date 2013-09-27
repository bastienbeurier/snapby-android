package com.streetshout.android.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.widget.Toast;
import com.streetshout.android.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImageUtils {
    public static boolean isSDPresent() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static File getFileToStoreImage() {
        File photostorage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(photostorage, (System.currentTimeMillis()) + ".jpg");
    }

    public static Intent getPhotoChooserIntent(Context ctx, File photoFile) {
        List<Intent> cameraIntents = new ArrayList<Intent>();

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
        cameraIntents.add(cameraIntent);

        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        Intent chooserIntent = Intent.createChooser(galleryIntent, ctx.getString(R.string.select_picture_from));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

        return chooserIntent;
    }

    static public Bitmap shrinkBitmapFromFile(String file, int width, int height) {

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

    static public void savePictureToGallery(Context ctx, String photoPath) {
        try {
            String photoName = GeneralUtils.getDeviceId(ctx) + "--" + (new Date()).getTime() + ".jpg";
            MediaStore.Images.Media.insertImage(ctx.getContentResolver(), photoPath, photoName, "Shout photo");
        } catch (FileNotFoundException e) {
            Toast.makeText(ctx, ctx.getString(R.string.failed_saving_photo), Toast.LENGTH_SHORT ).show();
        }
    }
}
