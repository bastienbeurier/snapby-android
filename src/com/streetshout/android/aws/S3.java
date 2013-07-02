/*
 * Copyright 2010-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.streetshout.android.aws;

import android.graphics.Bitmap;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.streetshout.android.activities.NewShoutContentActivity;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.ImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class S3 {

	private static ObjectListing objListing = null;

	public static AmazonS3Client getInstance() {
		return NewShoutContentActivity.clientManager.s3();
	}

    public static void addImageInBucket(String photoPath, String photoName) {
        try {
            Bitmap thumbImage = ImageUtils.shrinkBitmap(photoPath, Constants.SHOUT_THUMB_RES, Constants.SHOUT_THUMB_RES);
            Bitmap bigImage = ImageUtils.shrinkBitmap(photoPath, Constants.SHOUT_BIG_RES, Constants.SHOUT_BIG_RES);
            addImagewithRes(photoPath, photoName, thumbImage, Constants.SHOUT_THUMB_RES);
            addImagewithRes(photoPath, photoName, bigImage, Constants.SHOUT_BIG_RES);
        } catch (AmazonServiceException ex) {
            NewShoutContentActivity.clientManager.wipeCredentialsOnAuthError(ex);
            ex.printStackTrace();
        }
    }

    private static void addImagewithRes(String photoPath, String photoName, Bitmap bm, int res) {
        try {
            ImageUtils.storeBitmapInFile(photoPath, bm);
        } catch (Exception e) {
            e.printStackTrace();
        }

        PutObjectRequest por = new PutObjectRequest(Constants.PICTURE_BUCKET, photoName + "--" + res, new java.io.File(photoPath));
        getInstance().putObject(por);
    }

	protected static String read(InputStream stream) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(8196);
			byte[] buffer = new byte[1024];
			int length = 0;
			while ((length = stream.read(buffer)) > 0) {
				baos.write(buffer, 0, length);
			}

			return baos.toString();
		} catch (Exception exception) {
			return exception.getMessage();
		}
	}
}
