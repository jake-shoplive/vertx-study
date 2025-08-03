package com.gig.vertx.multiworkerpool;

import com.gig.vertx.multiworkerpool.client.WorkerVerticle;
import com.gig.vertx.multiworkerpool.config.DeploymentConfigFactory;
import com.gig.vertx.multiworkerpool.monitor.MessageQueueMonitor;
import com.gig.vertx.multiworkerpool.monitor.VertxMetricsMonitor;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static MessageQueueMonitor queueMonitor;
    private static VertxMetricsMonitor metricsMonitor;

    public static void main(String[] args) {
        logger.info("Starting Vertx application with comprehensive monitoring...");
        Vertx vertx = Vertx.vertx();

        // 메시지 큐 모니터링 초기화 (3초마다 상태 출력)
        queueMonitor = new MessageQueueMonitor(vertx, 3000);

        // Vertx 시스템 메트릭스 모니터링 초기화 (5초마다 상태 출력)
        metricsMonitor = new VertxMetricsMonitor(vertx, 5000);

        // Config를 사용하여 Worker Verticle 배포
        vertx.deployVerticle(
            WorkerVerticle.class.getName(),
            DeploymentConfigFactory.createDeploymentOptions(),
            res -> {
                if (res.succeeded()) {
                    logger.info("Worker Verticle deployment succeeded: {}", res.result());
                    // 배포 성공 후 상세 메트릭스 출력
                    metricsMonitor.logDetailedMetrics();
                } else {
                    logger.error("Worker Verticle deployment failed: {}", res.cause().getMessage(), res.cause());
                }
            }
        );

        vertx.setPeriodic(1000, id -> {
            String message = "Task at " + System.currentTimeMillis();
            vertx.eventBus().send("worker.task", message);
            queueMonitor.messageSent("worker.task");
            logger.debug("Sent task message: {}", message);
        });

        // 10초 후 상세 메트릭스 출력
        vertx.setTimer(10000, id -> {
            metricsMonitor.logDetailedMetrics();
        });
    }
}
