package com.gig.vertx.multiinstance.monitor;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class MessageQueueMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageQueueMonitor.class);
    
    private final AtomicLong totalSent = new AtomicLong(0);
    private final AtomicLong totalReceived = new AtomicLong(0);
    private final AtomicLong totalPending = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    
    private final Map<String, Long> addressStats = new ConcurrentHashMap<>();
    private final Map<String, Long> addressPending = new ConcurrentHashMap<>();
    
    private final Vertx vertx;
    private final long monitoringInterval;
    
    public MessageQueueMonitor(Vertx vertx, long monitoringIntervalMs) {
        this.vertx = vertx;
        this.monitoringInterval = monitoringIntervalMs;
        startMonitoring();
    }
    
    public void messageSent(String address) {
        totalSent.incrementAndGet();
        totalPending.incrementAndGet();
        addressStats.merge(address, 1L, Long::sum);
        addressPending.merge(address, 1L, Long::sum);
    }
    
    public void messageReceived(String address) {
        totalReceived.incrementAndGet();
        totalPending.decrementAndGet();
        addressPending.merge(address, -1L, Long::sum);
    }
    
    public void messageFailed(String address) {
        totalFailed.incrementAndGet();
        totalPending.decrementAndGet();
        addressPending.merge(address, -1L, Long::sum);
    }
    
    private void startMonitoring() {
        vertx.setPeriodic(monitoringInterval, id -> {
            logQueueStatus();
        });
    }
    
    private void logQueueStatus() {
        long sent = totalSent.get();
        long received = totalReceived.get();
        long pending = totalPending.get();
        long failed = totalFailed.get();
        long backlog = sent - received;
        
        logger.info("=== EventBus Queue Status ===");
        logger.info("Total Messages Sent: {}", sent);
        logger.info("Total Messages Received: {}", received);
        logger.info("Total Messages Pending: {}", pending);
        logger.info("Total Messages Failed: {}", failed);
        logger.info("Queue Backlog: {}", backlog);
        logger.info("Processing Rate: {}/sec", calculateProcessingRate());
        
        // 주소별 통계
        logger.info("--- Address Statistics ---");
        addressStats.forEach((address, count) -> {
            long pendingForAddress = addressPending.getOrDefault(address, 0L);
            logger.info("Address '{}': sent={}, pending={}", address, count, pendingForAddress);
        });
        
        // 경고 메시지
        if (backlog > 100) {
            logger.warn("⚠️  High queue backlog detected: {} messages", backlog);
        }
        if (pending > 50) {
            logger.warn("⚠️  High pending messages: {} messages", pending);
        }
        
        logger.info("===========================");
    }
    
    private double calculateProcessingRate() {
        // 간단한 처리율 계산 (실제로는 더 정교한 계산 필요)
        return (double) totalReceived.get() / (monitoringInterval / 1000.0);
    }
    
    public Map<String, Object> getQueueStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalSent", totalSent.get());
        stats.put("totalReceived", totalReceived.get());
        stats.put("totalPending", totalPending.get());
        stats.put("totalFailed", totalFailed.get());
        stats.put("backlog", totalSent.get() - totalReceived.get());
        stats.put("addressStats", new ConcurrentHashMap<>(addressStats));
        stats.put("addressPending", new ConcurrentHashMap<>(addressPending));
        return stats;
    }
} 