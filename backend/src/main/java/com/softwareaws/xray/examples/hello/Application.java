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
