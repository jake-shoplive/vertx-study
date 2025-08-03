package com.gig.vertx.multiworkerpool.client;

import com.gig.vertx.multiworkerpool.monitor.MessageQueueMonitor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(WorkerVerticle.class);
    private MessageQueueMonitor queueMonitor;

    @Override
    public void start() {
        JsonObject config = config();
        String delayThreadName = config.getString("delayThreadName", "worker-thread-1");
        int delayDuration = config.getInteger("delayDuration", 10000);
        int workerPoolSize = config.getInteger("workerPoolSize", 3); // config에서 풀 크기 가져오기

        logger.info("Worker Verticle started with config: {}", config.encode());
        logger.info("Worker pool size: {}", workerPoolSize);

        // 메시지 큐 모니터링 초기화
        queueMonitor = new MessageQueueMonitor(vertx, 2000);

        // 기본 워커 풀 사용 (DeploymentConfigFactory의 setWorkerPoolSize(3) 적용)
        logger.info("Using default worker pool with size: {}", workerPoolSize);

        // worker.task 주소로 전송된 메시지 처리
        vertx.eventBus().consumer("worker.task", msg -> {
            String message = (String) msg.body();
            logger.info("Received task: {} | Thread: {}", message, Thread.currentThread().getName());
            
            queueMonitor.messageReceived("worker.task");
            queueMonitor.taskStarted();

            // 즉시 응답하고 비동기로 작업 처리
            msg.reply("Task accepted");
            
            // 기본 워커 풀 사용 (DeploymentConfigFactory의 setWorkerPoolSize(3) 적용)
            vertx.executeBlocking(promise -> {
                    String threadName = Thread.currentThread().getName();
                    logger.info("Processing task on {}", threadName);

                    try {
                        // 특정 스레드에만 딜레이 적용
                        if (threadName.contains(delayThreadName)) {
                            logger.info(">> Delay on {} for {}ms", threadName, delayDuration);
                            Thread.sleep(delayDuration);
                        } else {
                            logger.info(">> Normal processing on {}", threadName);
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        promise.fail(e);
                        return;
                    }

                    promise.complete("Complete Task on " + threadName);
                }, res -> {
                    if (res.succeeded()) {
                        logger.info("✓ {}", res.result());
                        queueMonitor.taskCompleted();
                    } else {
                        logger.error("✗ Task failed: {}", res.cause());
                        queueMonitor.taskFailed();
                    }
                });
        });
    }

    /**
     * 기본 워커 풀 사용으로 인해 라운드 로빈 선택 로직 제거
     * DeploymentConfigFactory의 setWorkerPoolSize(3) 설정이 적용됨
     */
}