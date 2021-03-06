package pt.lunacloud.storage.client;

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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.lunacloud.LunacloudClientException;
import pt.lunacloud.LunacloudServiceException;
import pt.lunacloud.auth.ClasspathPropertiesFileCredentialsProvider;
import pt.lunacloud.services.storage.LunacloudStorage;
import pt.lunacloud.services.storage.LunacloudStorageClient;
import pt.lunacloud.services.storage.model.Bucket;
import pt.lunacloud.services.storage.model.GetObjectRequest;
import pt.lunacloud.services.storage.model.ListObjectsRequest;
import pt.lunacloud.services.storage.model.ObjectListing;
import pt.lunacloud.services.storage.model.PutObjectRequest;
import pt.lunacloud.services.storage.model.StorageObject;
import pt.lunacloud.services.storage.model.StorageObjectSummary;

/**
 * This sample demonstrates how to make basic requests to Lunacloud storage
 * using the AWS SDK for Java.
 * <p>
 * <b>Prerequisites:</b> You must have valid Lunacloud Storage Access and Secret
 * keys.
 */
public class StorageClient {

	private static final Logger logger = LoggerFactory
	        .getLogger(StorageClient.class);

	public static void main(String[] args) throws IOException {
		/*
		 * This credentials provider implementation loads your Lunacloud Storage
		 * credentials from a properties file at the root of your classpath.
		 * 
		 * Important: Be sure to fill in your access credentials in the
		 * LunacloudCredentials.properties file before you try to run this
		 * sample.
		 */
		LunacloudStorage s = new LunacloudStorageClient(
		        new ClasspathPropertiesFileCredentialsProvider());

		String bucketName = "my-first-bucket-" + UUID.randomUUID();
		String key = "MyObjectKey";

		logger.info("Getting Started with Lunacloud storage");

		try {
			/*
			 * Create a new bucket.
			 * 
			 * Bucket names are globally unique, so once a bucket name has been
			 * taken by any user, you can't create another bucket with that same
			 * name.
			 * 
			 * You can optionally specify a location for your bucket if you want
			 * to keep your data closer to your applications or users.
			 */
			logger.info("Creating bucket {}...\n", bucketName);
			s.createBucket(bucketName);

			// list the buckets in your account
			logger.info("Listing buckets");
			for (Bucket bucket : s.listBuckets())
				logger.info(" - {}\n", bucket.getName());

			/*
			 * Upload an object to your bucket.
			 * 
			 * You can easily upload a file to Storage, or upload directly an
			 * InputStream if you know the length of the data in the stream. You
			 * can also specify your own metadata when uploading to Storage,
			 * which allows you set a variety of options like content-type and
			 * content-encoding, plus additional metadata specific to your
			 * applications.
			 */
			logger.info("Uploading a new object to Lunacloud Storage from a file\n");
			s.putObject(new PutObjectRequest(bucketName, key,
			        createSampleFile()));

			/*
			 * Download an object
			 * 
			 * When you download an object, you get all of the object's metadata
			 * and a stream from which to read the contents. It's important to
			 * read the contents of the stream as quickly as possibly since the
			 * data is streamed directly from Lunacloud Storage and your network
			 * connection will remain open until you read all the data or close
			 * the input stream.
			 * 
			 * GetObjectRequest also supports several other options, including
			 * conditional downloading of objects based on modification times,
			 * ETags, and selectively downloading a range of an object.
			 */
			logger.info("Downloading an object");
			StorageObject object = s.getObject(new GetObjectRequest(bucketName,
			        key));
			System.out.println("Content-Type: "
			        + object.getObjectMetadata().getContentType());
			displayTextInputStream(object.getObjectContent());

			/*
			 * List objects in your bucket by prefix - There are many options
			 * for listing the objects in your bucket. Keep in mind that buckets
			 * with many objects might truncate their results when listing their
			 * objects, so be sure to check if the returned object listing is
			 * truncated, and use the listNextBatchOfObjects(...) operation to
			 * retrieve additional results.
			 */
			logger.info("Listing objects");
			ObjectListing objectListing = s
			        .listObjects(new ListObjectsRequest().withBucketName(
			                bucketName).withPrefix("My"));
			for (StorageObjectSummary objectSummary : objectListing
			        .getObjectSummaries())
				logger.info(
				        " - {}, size:{}",
				        new Object[] { objectSummary.getKey(),
				                objectSummary.getSize() });

			/*
			 * Delete an object.
			 * 
			 * Unless versioning has been turned on for your bucket, there is no
			 * way to undelete an object, so use caution when deleting objects.
			 */
			logger.info("Deleting an object\n");
			s.deleteObject(bucketName, key);

			/*
			 * Delete a bucket.
			 * 
			 * A bucket must be completely empty before it can be deleted, so
			 * remember to delete any objects from your buckets before you try
			 * to delete them.
			 */
			logger.info("Deleting bucket {}...\n", bucketName);
			s.deleteBucket(bucketName);
		} catch (LunacloudServiceException lse) {
			logger.error(
			        "Caught an LunacloudServiceException, which means your request made it "
			                + "to Lunacloud Storage, but was rejected with an error response for some reason.",
			        lse);
			logger.error("Error Message:    " + lse.getMessage());
			logger.error("HTTP Status Code: " + lse.getStatusCode());
			logger.error("Error Code:   " + lse.getErrorCode());
			logger.error("Error Type:       " + lse.getErrorType());
			logger.error("Request ID:       " + lse.getRequestId());
		} catch (LunacloudClientException lce) {
			logger.error(
			        "Caught an LunacloudClientException, which means the client encountered "
			                + "a serious internal problem while trying to communicate with Storage, "
			                + "such as not being able to access the network.",
			        lce);
		}
	}

	/**
	 * Creates a temporary file with text data to demonstrate uploading a file
	 * to Lunacloud Storage
	 * 
	 * @return A newly created temporary file with text data.
	 * 
	 * @throws IOException
	 */
	private static File createSampleFile() throws IOException {
		File file = File.createTempFile("lunacloud-java-sdk-", ".txt");
		file.deleteOnExit();

		Writer writer = new OutputStreamWriter(new FileOutputStream(file));
		writer.write("abcdefghijklmnopqrstuvwxyz\n");
		writer.write("01234567890112345678901234\n");
		writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
		writer.write("01234567890112345678901234\n");
		writer.write("abcdefghijklmnopqrstuvwxyz\n");
		writer.close();

		return file;
	}

	/**
	 * Displays the contents of the specified input stream as text.
	 * 
	 * @param input
	 *            The input stream to display as text.
	 * 
	 * @throws IOException
	 */
	private static void displayTextInputStream(InputStream input)
	        throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;

			logger.info("    {}", line);
		}
		reader.close();
	}

}