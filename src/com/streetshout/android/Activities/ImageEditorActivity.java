package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.edmodo.cropper.CropImageView;
import com.streetshout.android.R;
import com.streetshout.android.aws.S3;
import com.streetshout.android.tvmclient.Response;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.ImageUtils;

public class ImageEditorActivity extends Activity {

    private static final int ROTATE_NINETY_DEGREES = 90;

    private CropImageView cropImageView = null;
    private Button rotateButton = null;
    private Button cropButton = null;

    private int mAspectRatioX = 10;
    private int mAspectRatioY = 10;

    private Bitmap croppedImage = null;
    private Bitmap shrinkedCroppedImage = null;

    private String photoPath = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.image_editor);

        photoPath = getIntent().getStringExtra("photoPath");
        croppedImage = BitmapFactory.decodeFile(photoPath);

        cropImageView = (CropImageView) findViewById(R.id.crop_image_view);
        cropImageView.setImageBitmap(croppedImage);

        cropImageView.setAspectRatio(mAspectRatioX, mAspectRatioY);
        cropImageView.setFixedAspectRatio(true);

        rotateButton = (Button) findViewById(R.id.rotate_button);

        rotateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                cropImageView.rotateImage(ROTATE_NINETY_DEGREES);
            }
        });

        cropButton = (Button) findViewById(R.id.crop_button);

        cropButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                croppedImage = cropImageView.getCroppedImage();
                Log.d("BAB", "BYTE COUNNNNTTTT BEFORE: " + croppedImage.getByteCount());

                ImageUtils.storeBitmapInFile(photoPath, croppedImage);

                croppedImage = ImageUtils.shrinkBitmapFromFile(photoPath, Constants.SHOUT_BIG_RES, Constants.SHOUT_BIG_RES);

                Log.d("BAB", "BYTE COUNNNNTTTT AFTER: " + croppedImage.getByteCount());

                final Intent returnIntent = new Intent();
                returnIntent.putExtra("croppedImage", croppedImage);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });
    }
}