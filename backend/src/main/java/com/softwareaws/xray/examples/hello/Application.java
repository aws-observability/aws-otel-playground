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

package com.softwareaws.xray.examples.hello;

import io.grpc.ServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Spark;

public class Application {

    private static final Logger logger = LogManager.getLogger();

    private static void setupSpark() {
        Spark.port(8083);
        Spark.get("/hellospark", (req, res) -> "ok");
    }

    public static void main(String[] args) throws Exception {
        var server = ServerBuilder.forPort(8081)
                                  .addService(new HelloService())
                                  .build();

        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdownNow();
            Spark.stop();
        }));

        logger.info("Server started at port 8081.");

        setupSpark();

        server.awaitTermination();
    }
}
