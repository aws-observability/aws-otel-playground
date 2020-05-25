/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.softwareaws.xray.opentelemetry.exporters.resources;

import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.resources.ResourceConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Ec2ResourcePopulator extends ResourcePopulator {

    private static final Logger logger = LoggerFactory.getLogger(Ec2ResourcePopulator.class);

    private static final int TIMEOUT_MILLIS = 2000;
    private static final String DEFAULT_IDMS_ENDPOINT = "169.254.169.254";

    private final URL tokenUrl;
    private final URL instanceIdUrl;
    private final URL availabilityZoneUrl;

    Ec2ResourcePopulator() {
        this(System.getenv("IMDS_ENDPOINT") != null ? System.getenv("IMDS_ENDPOINT") : DEFAULT_IDMS_ENDPOINT);
    }

    Ec2ResourcePopulator(String endpoint) {
        String urlBase = "http://" + endpoint;
        try {
            this.tokenUrl = new URL(urlBase + "/latest/api/token");
            this.instanceIdUrl = new URL(urlBase + "/latest/meta-data/instance-id");
            this.availabilityZoneUrl = new URL(urlBase + "/latest/meta-data/placement/availability-zone");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Illegal endpoint: " + endpoint);
        }
    }

    @Override
    public void populate(Map<String, AttributeValue> resourceAttributes) {
        String token = fetchToken();

        // If token is empty, either IDMSv2 isn't enabled or an unexpected failure happened. We can still get
        // data if IDMSv1 is enabled.
        String instanceId = fetchInstanceId(token);
        if (instanceId.isEmpty()) {
            // If no instance ID, assume we are not actually running on EC2.
            return;
        }

        resourceAttributes.put(ResourceConstants.HOST_ID, attributeValue(instanceId));

        String availabilityZone = fetchAvailabilityZone(token);
        resourceAttributes.put(ResourceConstants.CLOUD_ZONE, attributeValue(availabilityZone));

        resourceAttributes.put(ResourceConstants.CLOUD_PROVIDER, attributeValue("aws"));
    }

    private String fetchToken() {
        return fetchString("PUT", tokenUrl, "", true);
    }

    private String fetchInstanceId(String token) {
        return fetchString("GET", instanceIdUrl, token, false);
    }

    private String fetchAvailabilityZone(String token) {
        return fetchString("GET", availabilityZoneUrl, token, false);
    }

    // Generic HTTP fetch function for IDMS.
    private static String fetchString(String httpMethod, URL url, String token, boolean includeTtl) {
        final HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            logger.warn("Error connecting to IDMS.", e);
            return "";
        }

        try {
            connection.setRequestMethod(httpMethod);
        } catch (ProtocolException e) {
            logger.warn("Unknown HTTP method, this is a programming bug.", e);
            return "";
        }

        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);

        if (includeTtl) {
            connection.setRequestProperty("X-aws-ec2-metadata-token-ttl-seconds", "60");
        }
        if (!token.isEmpty()) {
            connection.setRequestProperty("X-aws-ec2-metadata-token", token);
        }

        final int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (Exception e) {
            logger.warn("Error connecting to IDMS.", e);
            return "";
        }

        if (responseCode != 200) {
            logger.warn("Error reponse from IDMS: code (" + responseCode + ") text " + readResponseString(connection));
        }

        return readResponseString(connection).trim();
    }

    private static String readResponseString(HttpURLConnection connection) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (InputStream is = connection.getInputStream()) {
            readTo(is, os);
        } catch (IOException e) {
            // Only best effort read if we can.
        }
        try (InputStream is = connection.getErrorStream()) {
            readTo(is, os);
        } catch (IOException e) {
            // Only best effort read if we can.
        }
        try {
            return os.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported can't happen.");
        }
    }

    private static void readTo(@Nullable InputStream is, ByteArrayOutputStream os) throws IOException {
        if (is == null) {
            return;
        }
        int b;
        while ((b = is.read()) != -1) {
            os.write(b);
        }
    }

    private static AttributeValue attributeValue(String value) {
        return AttributeValue.stringAttributeValue(!value.isEmpty() ? value : "unknown");
    }
}
