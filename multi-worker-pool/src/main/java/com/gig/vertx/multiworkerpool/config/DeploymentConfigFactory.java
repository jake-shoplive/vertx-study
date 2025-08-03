package com.gig.vertx.multiworkerpool.config;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

public class DeploymentConfigFactory {

    public static DeploymentOptions createDeploymentOptions() {
        JsonObject config = new JsonObject()
            .put("workerPoolName", "test-worker-pool")
            .put("delayThreadName", "worker-thread-1")  // 2번째 워커 풀에 딜레이
            .put("delayDuration", 10000);  // 10초 딜레이

        return new DeploymentOptions()
            .setInstances(1)  // 1개 인스턴스
            .setWorker(true)
            .setWorkerPoolName("worker-thread")
            .setWorkerPoolSize(5)  // 3개의 워커 풀
            .setConfig(config);
    }
}
