package com.rickey.gateway.filter;

import cn.hutool.core.text.AntPathMatcher;
import com.rickey.clientSDK.utils.SignUtils;
import com.rickey.common.model.entity.InterfaceInfo;
import com.rickey.common.model.entity.User;
import com.rickey.common.service.InnerInterfaceInfoService;
import com.rickey.common.service.InnerUserInterfaceInfoService;
import com.rickey.common.service.InnerUserService;
import com.rickey.gateway.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局过滤器，用于处理请求和响应的日志、鉴权、限流等功能
 */
@Slf4j
@Component
@CrossOrigin(origins = "*")
public class InterfaceInvokeFilter implements GlobalFilter, Ordered {

    @DubboReference
    private InnerUserService innerUserService; // 用户服务接口

    @DubboReference
    private InnerInterfaceInfoService innerInterfaceInfoService; // 接口信息服务接口

    @DubboReference
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService; // 用户接口信息服务接口

    private final RedisUtil redisUtil;

    private static final String INTERFACE_HOST = "http://localhost:8123"; // 接口的主机地址


    @Autowired
    public InterfaceInvokeFilter(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String webPath = request.getPath().value();
        log.info("InterfaceInvokeFilter:请求路径:{}", webPath);

        // 只在路径为 /api/interfaceInvoke/** 时进行过滤
        AntPathMatcher pathMatcher = new AntPathMatcher();
        if (!pathMatcher.match("/api/interfaceInvoke/**", webPath)) {
            log.info("InterfaceInvokeFilter Skip成功, 当前路径不需要过滤");
            return chain.filter(exchange);
        }

        // 1. 请求日志记录
        String path = INTERFACE_HOST + request.getPath().value(); // 请求路径
        String method = request.getMethod().toString(); // 请求方法
        log.info("请求唯一标识：{}", request.getId());
        log.info("请求路径：{}", path);
        log.info("请求方法：{}", method);
        log.info("请求参数：{}", request.getQueryParams());
        String sourceAddress = request.getLocalAddress().getHostString(); // 本地地址
        log.info("请求来源地址：{}", sourceAddress);
        log.info("请求来源地址：{}", request.getRemoteAddress()); // 远程地址
        ServerHttpResponse response = exchange.getResponse();

        // 3. 用户鉴权（判断 ak、sk 是否合法）
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey"); // 获取 accessKey
        String nonce = headers.getFirst("nonce"); // 获取 nonce
        String timestamp = headers.getFirst("timestamp"); // 获取时间戳
        String sign = headers.getFirst("sign"); // 获取签名
        String body = headers.getFirst("body"); // 获取请求体


        log.info("接收到请求的头部信息：accessKey = {}, nonce = {}, timestamp = {}, sign = {}, body = {}",
                accessKey, nonce, timestamp, sign, body);

        User invokeUser = null;

        // 从数据库中查找用户
        try {
            invokeUser = innerUserService.getInvokeUser(accessKey);
            log.info("用户信息：{}", invokeUser); // 记录用户信息
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
        }

        // 如果用户不存在，返回未授权
        if (invokeUser == null) {
            log.error("用户不存在，返回未授权");
            return handleNoAuth(response);
        }

        // 检查 nonce 是否有效
        try {
            long nonceValue = Long.parseLong(nonce);
            if (nonceValue > 10000L) {
                log.error("nonce 超过最大值，拒绝请求：nonce = {}", nonce);
                return handleNoAuth(response);
            }
        } catch (NumberFormatException e) {
            log.error("nonce 格式不正确", e);
            return handleNoAuth(response);
        }

        // 检查时间戳是否超时
        try {
            Long currentTime = System.currentTimeMillis() / 1000;
            final Long FIVE_MINUTES = 60 * 5L;
            if ((currentTime - Long.parseLong(timestamp)) >= FIVE_MINUTES) {
                log.error("请求超时，时间戳超出有效范围：timestamp = {}", timestamp);
                return handleNoAuth(response);
            }
        } catch (NumberFormatException e) {
            log.error("时间戳格式不正确", e);
            return handleNoAuth(response);
        }

        // 从数据库中查找用户的 secretKey
        String secretKey = invokeUser.getSecretKey();
        String serverSign = SignUtils.genSign(body, secretKey);

        // 验证签名是否匹配
        if (sign == null || !sign.equals(serverSign)) {
            log.error("签名验证失败，客户端签名：{}, 服务器生成签名：{}", sign, serverSign);
            return handleNoAuth(response);
        }

        InterfaceInfo interfaceInfo = null;

        // 4. 检查请求的接口是否存在，以及请求方法是否匹配
        try {
            interfaceInfo = innerInterfaceInfoService.getInterfaceInfo(path, method);
            log.info("接口信息：{}", interfaceInfo);
        } catch (Exception e) {
            log.error("获取接口信息失败", e);
        }

        // 如果接口不存在，返回未授权
        if (interfaceInfo == null) {
            log.error("接口不存在，path = {}, method = {}", path, method);
            return handleNoAuth(response);
        }

        // 根据UserInterfaceInfo表检查用户是否还有剩余的调用次数
        long remainingCalls = innerUserInterfaceInfoService.getApiRemainingCalls(interfaceInfo.getId(), invokeUser.getId());
        log.info("剩余调用次数：{}", remainingCalls);
        if (remainingCalls <= 0) {
            log.error("调用次数不足，接口：{}, 用户：{}", interfaceInfo.getId(), invokeUser.getId());
            return handleInvokeLimitError(response);
        }

        // 6.利用Redis做分布式锁，同一用户在同一时间只能调用一个接口
        String redisKey = invokeUser.getId() + ":invoke:" + interfaceInfo.getId(); // 生成 Redis Key

        // Boolean b = redisTemplate.opsForValue().setIfAbsent(redisKey, String.valueOf(remainingCalls), 10, TimeUnit.SECONDS);
        boolean setNXSuccess = redisUtil.setNX(redisKey, String.valueOf(remainingCalls), 5);
        log.info("尝试获取 Redis 锁，redisKey = {}, value = {}", redisKey, remainingCalls);
        if (!setNXSuccess) {
            log.error("调用过于频繁，Redis 锁获取失败：redisKey = {}", redisKey);
            return handleInvokeTooFrequentlyError(response);
        }

        log.info("Redis 锁成功获取，redisKey = {}, value = {}", redisKey, remainingCalls);

        // 处理请求响应
        return handleResponse(exchange, chain, interfaceInfo.getId(), invokeUser.getId());
    }


    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain, long interfaceInfoId, long userId) {
        try {
            ServerHttpResponse originalResponse = exchange.getResponse(); // 获取原始响应
            DataBufferFactory bufferFactory = originalResponse.bufferFactory(); // 缓存数据的工厂
            HttpStatus statusCode = originalResponse.getStatusCode(); // 获取响应状态码

            // 只有在调用成功的时候才会进行调用次数的增加
            if (statusCode == HttpStatus.OK) {
                log.info("响应成功，准备扣减接口调用次数");

                // 装饰响应以增强功能
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        // 确保流数据只消费一次
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);

                            return super.writeWith(
                                    fluxBody.buffer()  // 使用 buffer 来确保流数据被完整消费一次
                                            .flatMap(dataBuffers -> {
                                                // 合并所有的数据块
                                                byte[] content = new byte[dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum()];
                                                int offset = 0;

                                                // 将所有的 dataBuffer 合并为一个 byte[] 内容
                                                for (DataBuffer dataBuffer : dataBuffers) {
                                                    int length = dataBuffer.readableByteCount();
                                                    dataBuffer.read(content, offset, length);
                                                    offset += length;

                                                    // 释放每个 dataBuffer
                                                    DataBufferUtils.release(dataBuffer);
                                                }

                                                // 进行日志记录、调用次数更新等操作
                                                String redisKey = userId + ":invoke:" + interfaceInfoId;
                                                log.info("redisKey: {}", redisKey);

                                                // 删除 Redis 键值对
                                                boolean delRedisKeySuccess = redisUtil.del(redisKey);
                                                log.info("删除 redisKey, 结果 = {}", delRedisKeySuccess);

                                                // 如果响应成功
                                                if (statusCode == HttpStatus.OK) {
                                                    // 更新调用次数
                                                    boolean invokeCountSuccess = innerUserInterfaceInfoService.invokeCount(interfaceInfoId, userId);
                                                    log.info("更新调用次数，结果 = {}", invokeCountSuccess);
                                                } else {
                                                    // 响应失败，设置错误响应
                                                    String errorMessage = "{\"error\": \"参数填写错误，请检查参数\"}";
                                                    content = errorMessage.getBytes(StandardCharsets.UTF_8);
                                                }

                                                // 返回修改后的响应体
                                                return Mono.just(bufferFactory.wrap(content));
                                            })
                            );
                        }

                        // 如果不是 Flux 流，则直接调用原始的响应体处理
                        return super.writeWith(body);
                    }
                };

                // 设置 response 对象为装饰过的
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }

            // 如果响应状态不是 OK，直接返回原始响应
            return chain.filter(exchange); // 降级处理返回数据
        } catch (Exception e) {
            log.error("网关处理响应异常", e);
            return chain.filter(exchange); // 返回原始响应
        }
    }

    // 处理未授权的请求
    public Mono<Void> handleNoAuth(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN); // 设置响应状态为403
        return response.setComplete(); // 完成响应
    }

    // 处理调用次数用尽的错误
    public Mono<Void> handleInvokeLimitError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN); // 设置响应状态为403
        String message = "您没有调用次数了，请先获取调用次数！"; // 错误信息
        DataBuffer buffer = response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8)); // 包装错误信息
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8"); // 设置响应头
        return response.writeWith(Mono.just(buffer)); // 返回错误信息
    }

    // 处理调用过于频繁的错误、调用次数用尽的错误
    public Mono<Void> handleInvokeTooFrequentlyError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN); // 设置响应状态为403
        String message = "您的调用过于频繁，请稍后重试"; // 错误信息
        DataBuffer buffer = response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8)); // 包装错误信息
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8"); // 设置响应头
        return response.writeWith(Mono.just(buffer)); // 返回错误信息
    }

    // 处理调用过于频繁的错误、调用次数用尽的错误
    public Mono<Void> handleParamsError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN); // 设置响应状态为403
        String message = "参数填写错误，请检查输入参数"; // 错误信息
        DataBuffer buffer = response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8)); // 包装错误信息
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8"); // 设置响应头
        return response.writeWith(Mono.just(buffer)); // 返回错误信息
    }

    @Override
    public int getOrder() {
        return -1; // 指定过滤器的执行顺序
    }
}