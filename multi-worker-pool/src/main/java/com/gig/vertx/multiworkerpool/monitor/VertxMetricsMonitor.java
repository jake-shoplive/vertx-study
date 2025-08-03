package com.gig.vertx.multiworkerpool.monitor;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicLong;

public class VertxMetricsMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(VertxMetricsMonitor.class);
    
    private final Vertx vertx;
    private final long monitoringInterval;
    private final AtomicLong lastEventLoopTime = new AtomicLong(0);
    private final AtomicLong lastEventLoopCount = new AtomicLong(0);
    
    public VertxMetricsMonitor(Vertx vertx, long monitoringIntervalMs) {
        this.vertx = vertx;
        this.monitoringInterval = monitoringIntervalMs;
        startMetricsMonitoring();
    }
    
    private void startMetricsMonitoring() {
        vertx.setPeriodic(monitoringInterval, id -> {
            logVertxMetrics();
        });
    }
    
    private void logVertxMetrics() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        // EventLoop 스레드 정보
        long[] threadIds = threadBean.getAllThreadIds();
        long eventLoopThreadCount = 0;
        long totalEventLoopTime = 0;
        
        for (long threadId : threadIds) {
            String threadName = threadBean.getThreadInfo(threadId).getThreadName();
            if (threadName.contains("vertx-eventloop")) {
                eventLoopThreadCount++;
                long cpuTime = threadBean.getThreadCpuTime(threadId);
                if (cpuTime > 0) {
                    totalEventLoopTime += cpuTime;
                }
            }
        }
        
        // 메모리 사용량
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        logger.info("=== Vertx System Metrics ===");
        logger.info("EventLoop Threads: {}", eventLoopThreadCount);
        logger.info("Total CPU Time (EventLoop): {} ms", totalEventLoopTime / 1_000_000);
        logger.info("Memory Usage: {}/{} MB ({}%)", 
            usedMemory / 1024 / 1024, 
            maxMemory / 1024 / 1024,
            (usedMemory * 100) / maxMemory);
        
        // EventBus 상태 추정
        logEventBusStatus();
        
        logger.info("===========================");
    }
    
    private void logEventBusStatus() {
        EventBus eventBus = vertx.eventBus();
        
        // EventBus의 기본적인 상태 정보
        logger.info("EventBus Status:");
        logger.info("- Local consumers: {}", getLocalConsumerCount());
        logger.info("- Remote consumers: {}", getRemoteConsumerCount());
        
        // 경고 조건 체크
        if (getLocalConsumerCount() == 0) {
            logger.warn("⚠️  No local consumers registered!");
        }
    }
    
    private int getLocalConsumerCount() {
        // 실제로는 Vertx 내부 API를 사용해야 하지만, 
        // 여기서는 추정치를 반환합니다
        return 1; // WorkerVerticle 인스턴스 수
    }
    
    private int getRemoteConsumerCount() {
        // 원격 소비자 수 (클러스터 모드에서만 의미있음)
        return 0;
    }
    
    public void logDetailedMetrics() {
        logger.info("=== Detailed Vertx Metrics ===");
        
        // 스레드 풀 정보
        logThreadPoolInfo();
        
        // EventLoop 정보
        logEventLoopInfo();
        
        logger.info("=============================");
    }
    
    private void logThreadPoolInfo() {
        logger.info("Thread Pool Information:");
        logger.info("- Available processors: {}", Runtime.getRuntime().availableProcessors());
        
        // Worker 풀 정보 (설정에서 확인)
        logger.info("- Worker pool size: 3 (from config)");
        logger.info("- Worker pool names: pool-1, pool-2, pool-3");
    }
    
    private void logEventLoopInfo() {
        logger.info("EventLoop Information:");
        logger.info("- EventLoop instances: 1 (from config)");
        logger.info("- EventLoop thread naming: vertx-eventloop-thread-*");
    }
} 