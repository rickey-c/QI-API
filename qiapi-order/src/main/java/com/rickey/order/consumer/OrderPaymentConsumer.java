package com.rickey.order.consumer;

import com.rickey.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "order-topic", consumerGroup = "order-consumer-group")
public class OrderPaymentConsumer implements RocketMQListener<String> {

    private final OrderService orderService;

    private final RocketMQTemplate rocketMQTemplate;

    private final String DEAD_LETTER_TOPIC = "%DLQ%order-topic:consumeUpdateMessage";

    @Autowired
    public OrderPaymentConsumer(OrderService orderService, RocketMQTemplate rocketMQTemplate) {
        this.orderService = orderService;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * @param message
     */
    @Override
    public void onMessage(String message) {
        long orderId = Long.parseLong(message);
        log.info("Order ID: {}", orderId);

        int maxRetryCount = 3;  // 设置最大重试次数
        int retryCount = 0;
        boolean updateSuccess = false;

        while (retryCount < maxRetryCount) {
            // 尝试更新订单状态
            updateSuccess = orderService.updateOrderStatus(orderId);

            if (updateSuccess) {
                log.info("Order successfully updated on attempt {}", retryCount + 1);
                return; // 成功更新订单，直接返回
            }

            // 如果更新失败，增加重试次数
            retryCount++;
            log.info("Order update failed, attempt {}/{}", retryCount, maxRetryCount);

            // 如果是最后一次重试，停止再试并发送到死信队列
            if (retryCount >= maxRetryCount) {
                log.info("Max retry attempts reached, sending to dead letter queue");
                sendToDeadLetterQueue(DEAD_LETTER_TOPIC, message);
                break;
            }

            try {
                // 可以选择等待一段时间后再进行重试，防止过于频繁的重试
                Thread.sleep(1000 * retryCount);  // 增加退避时间，延迟1秒，2秒，3秒
            } catch (InterruptedException e) {
                log.error("Error occurred while waiting before retry", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void sendToDeadLetterQueue(String topic, String message) {
        // 假设有一个死信队列服务
        rocketMQTemplate.convertAndSend(topic, message);
    }
}
