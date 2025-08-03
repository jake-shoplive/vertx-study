package com.gig.vertx.multiinstance;

import com.gig.vertx.multiinstance.client.DeploymentVerticle;
import com.gig.vertx.multiinstance.config.DeploymentConfigFactory;
import com.gig.vertx.multiinstance.monitor.MessageQueueMonitor;
import com.gig.vertx.multiinstance.monitor.VertxMetricsMonitor;
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

        vertx.deployVerticle(
            DeploymentVerticle.class.getName(),
            DeploymentConfigFactory.createDeploymentOptions(),
            res -> {
                if (res.succeeded()) {
                    logger.info("Deployment succeeded: {}", res.result());
                    // 배포 성공 후 상세 메트릭스 출력
                    metricsMonitor.logDetailedMetrics();
                } else {
                    logger.error("Deployment failed: {}", res.cause().getMessage(), res.cause());
                }
            }
        );

        vertx.setPeriodic(1000, id -> {
            String message = "Hello at " + System.currentTimeMillis();
            vertx.eventBus().send("demo.address", message);
            queueMonitor.messageSent("demo.address");
            logger.debug("Sent message: {}", message);
        });
        
        // 10초 후 상세 메트릭스 출력
        vertx.setTimer(10000, id -> {
            metricsMonitor.logDetailedMetrics();
        });
    }
}
