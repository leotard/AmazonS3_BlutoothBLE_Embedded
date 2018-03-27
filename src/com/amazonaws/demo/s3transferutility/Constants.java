/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazonaws.demo.s3transferutility;

public class Constants {

    /*
     * You should replace these values with your own. See the README for details
     * on what to fill in.
     */
    public static final String COGNITO_POOL_ID = "us-east-1:80961f80-400d-448f-8482-261d30243a9e";
//Leo:"us-west-2:3e091094-f66b-4709-8c2e-dcf5483e215b"
    //Ryan: "us-east-1:80961f80-400d-448f-8482-261d30243a9e"
    /*
     * Region of your Cognito identity pool ID.
     */
    public static final String COGNITO_POOL_REGION = "us-east-1";
        //Leo: "us-west-2";
    //Ryan : "us-east-1"

    /*
     * Note, you must first create a bucket using the S3 console before running
     * the sample (https://console.aws.amazon.com/s3/). After creating a bucket,
     * put it's name in the field below.
     */
    public static final String BUCKET_NAME = "acceledata";//Leo: leobb
//Ryan : "acceledata"
    /*
     * Region of your bucket.
     */
    public static final String BUCKET_REGION = "us-east-1";
}
