package com.gig.vertx.multiinstance.config;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

public class DeploymentConfigFactory {

    public static DeploymentOptions createDeploymentOptions() {
        JsonObject config = new JsonObject()
            .put("message", "This is from DeploymentOptions!");

        return new DeploymentOptions()
            .setInstances(4)
            .setWorker(true)
            .setWorkerPoolName("custom-pool")
            .setWorkerPoolSize(8)
            .setConfig(config);
    }
}
