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
import android.net.Uri;
import android.util.Log;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.streetshout.android.activities.NewShoutContentActivity;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.ImageUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class S3 {

	private static ObjectListing objListing = null;

	public static AmazonS3Client getInstance() {
		return NewShoutContentActivity.clientManager.s3();
	}

    public static void addImageInBucket(String photoPath, String photoName) {
        try {
            try {
                ImageUtils.storeBitmapInFile(photoPath, ImageUtils.shrinkBitmap(photoPath, 400, 400));
                PutObjectRequest por = new PutObjectRequest(Constants.PICTURE_BUCKET, photoName, new java.io.File(photoPath));
                getInstance().putObject(por);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Request a URL for this image
            ResponseHeaderOverrides override = new ResponseHeaderOverrides();override.setContentType( "image/jpeg" );
            GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(Constants.PICTURE_BUCKET, photoName);
            //Url expires in 24 hours
            urlRequest.setExpiration(new Date(System.currentTimeMillis() + Constants.SHOUT_DURATION));
            urlRequest.setResponseHeaders(override);
            URL url = getInstance().generatePresignedUrl(urlRequest);

            //TODO: remove
            Log.d("BAB", "Photo url: " + url);

        } catch (AmazonServiceException ex) {
            NewShoutContentActivity.clientManager.wipeCredentialsOnAuthError(ex);
            ex.printStackTrace();
        }
    }

	public static List<String> getBucketNames() {

		try {
			List<Bucket> buckets = getInstance().listBuckets();

			List<String> bucketNames = new ArrayList<String>(buckets.size());
			Iterator<Bucket> bIter = buckets.iterator();
			while (bIter.hasNext()) {
				bucketNames.add((bIter.next().getName()));
			}
			return bucketNames;

		} catch (AmazonServiceException ex) {
			NewShoutContentActivity.clientManager.wipeCredentialsOnAuthError(ex);
		}

		return null;
	}

	public static List<String> getObjectNamesForBucket(String bucketName) {

		try {
			ObjectListing objects = getInstance().listObjects(bucketName);
			objListing = objects;
			List<String> objectNames = new ArrayList<String>(objects
					.getObjectSummaries().size());
			Iterator<S3ObjectSummary> oIter = objects.getObjectSummaries()
					.iterator();
			while (oIter.hasNext()) {
				objectNames.add(oIter.next().getKey());
			}
			// By default list objects will only return 1000 keys
			// This code will make multiple calls to fetch all keys in a bucket
			// NOTE: This could potentially cause an out of memory error
			while (objects.isTruncated()) {
				objects = getInstance().listNextBatchOfObjects(objects);
				oIter = objects.getObjectSummaries().iterator();
				while(oIter.hasNext()){
					objectNames.add(oIter.next().getKey());
				}
			}
			return objectNames;

		} catch (AmazonServiceException ex) {
			NewShoutContentActivity.clientManager.wipeCredentialsOnAuthError(ex);
		}

		return null;
	}

	public static List<String> getObjectNamesForBucket(String bucketName,
			int numItems) {

		try {
			ListObjectsRequest req = new ListObjectsRequest();
			req.setMaxKeys(new Integer(numItems));
			req.setBucketName(bucketName);
			ObjectListing objects = getInstance().listObjects(req);
			objListing = objects;
			List<String> objectNames = new ArrayList<String>(objects
					.getObjectSummaries().size());
			Iterator<S3ObjectSummary> oIter = objects.getObjectSummaries()
					.iterator();
			while (oIter.hasNext()) {
				objectNames.add(oIter.next().getKey());
			}

			return objectNames;

		} catch (AmazonServiceException ex) {
            NewShoutContentActivity.clientManager.wipeCredentialsOnAuthError(ex);
		}

		return null;
	}

	public static List<String> getMoreObjectNamesForBucket() {
		try {
			ObjectListing objects = getInstance().listNextBatchOfObjects(
					objListing);
			objListing = objects;
			List<String> objectNames = new ArrayList<String>(objects
					.getObjectSummaries().size());
			Iterator<S3ObjectSummary> oIter = objects.getObjectSummaries()
					.iterator();
			while (oIter.hasNext()) {
				objectNames.add(oIter.next().getKey());
			}
			return objectNames;
		} catch (NullPointerException e) {
			return new ArrayList<String>();
		} catch (AmazonServiceException ex) {
            NewShoutContentActivity.clientManager.wipeCredentialsOnAuthError(ex);
		}

		return null;
	}

	public static void createBucket(String bucketName) {
		try {
			getInstance().createBucket(bucketName);
		} catch (AmazonServiceException ex) {
            NewShoutContentActivity.clientManager.wipeCredentialsOnAuthError(ex);
		}
	}

	public static void deleteBucket(String bucketName) {
		try {
			getInstance().deleteBucket(bucketName);
		} catch (AmazonServiceException ex) {
            NewShoutContentActivity.clientManager.wipeCredentialsOnAuthError(ex);
		}
	}

	public static void createObjectForBucket(String bucketName,
			String objectName, String data) {

		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(
					data.getBytes());
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(data.getBytes().length);
			getInstance().putObject(bucketName, objectName, bais, metadata);

		} catch (AmazonServiceException ex) {

            NewShoutContentActivity.clientManager.wipeCredentialsOnAuthError(ex);

		} catch (Exception exception) {

			Log.e("TODO", "createObjectForBucket");

		}
	}

	public static void deleteObject(String bucketName, String objectName) {

		try {
			getInstance().deleteObject(bucketName, objectName);
		} catch (AmazonServiceException ex) {
            NewShoutContentActivity.clientManager.wipeCredentialsOnAuthError(ex);
		}
	}

	public static String getDataForObject(String bucketName, String objectName) {
		try {
			return read(getInstance().getObject(bucketName, objectName)
					.getObjectContent());
		} catch (AmazonServiceException ex) {
            NewShoutContentActivity.clientManager.wipeCredentialsOnAuthError(ex);
		}

		return null;
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
