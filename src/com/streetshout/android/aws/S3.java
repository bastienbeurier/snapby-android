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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.streetshout.android.activities.CreateShoutActivity;
import com.streetshout.android.utils.Constants;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class S3 {

	private static ObjectListing objListing = null;

	public static AmazonS3Client getInstance() {
		return CreateShoutActivity.clientManager.s3();
	}

    public static boolean addImageInBucket(String photoPath, String photoName) {
        try {
            return addImagewithRes(photoPath, photoName, Constants.SHOUT_BIG_RES);
        } catch (AmazonServiceException ex) {
            CreateShoutActivity.clientManager.wipeCredentialsOnAuthError(ex);
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean addImagewithRes(String photoPath, String photoName, int res) {
        try {
            PutObjectRequest por = new PutObjectRequest(Constants.PICTURE_BUCKET, photoName + "--" + res, new java.io.File(photoPath));
            getInstance().putObject(por);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
