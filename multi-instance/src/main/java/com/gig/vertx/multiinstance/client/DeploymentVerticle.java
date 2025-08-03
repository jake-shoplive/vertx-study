package com.gig.vertx.multiinstance.client;

import com.gig.vertx.multiinstance.monitor.MessageQueueMonitor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentVerticle.class);

    private static int counter = 0;
    private int instanceId;
    private MessageQueueMonitor queueMonitor;

    @Override
    public void start() {
        synchronized (DeploymentVerticle.class) {
            instanceId = ++counter; // 고유 인스턴스 번호
        }

        String threadName = Thread.currentThread().getName();
        JsonObject config = config();
        String message = config.getString("message", "No message provided");

        logger.info("[Instance {}] Started on thread: {}, message: {}", instanceId, threadName, message);

        // 메시지 큐 모니터링 인스턴스 가져오기
        queueMonitor = new MessageQueueMonitor(vertx, 5000);

        vertx.eventBus().consumer("demo.address", msg -> {
            logger.info("[Instance {}] Received: {} | Thread: {}", 
                instanceId, msg.body(), Thread.currentThread().getName());

            // 메시지 수신 알림
            queueMonitor.messageReceived("demo.address");

            if (instanceId == 2) {
                try {
                    logger.info("[Instance 2] Simulating delay...");
                    Thread.sleep(10000); // 10초 블로킹
                } catch (InterruptedException e) {
                    logger.error("Interrupted while sleeping", e);
                    queueMonitor.messageFailed("demo.address");
                    return;
                }
            }

            logger.info("[Instance {}] Done: {}", instanceId, msg.body());
        });
    }
}
