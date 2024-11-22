package com.rickey.gateway.qiapigateway;

import cn.hutool.core.text.AntPathMatcher;
import com.rickey.clientSDK.utils.SignUtils;
import com.rickey.common.model.entity.InterfaceInfo;
import com.rickey.common.model.entity.User;
import com.rickey.common.service.InnerInterfaceInfoService;
import com.rickey.common.service.InnerUserInterfaceInfoService;
import com.rickey.common.service.InnerUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.RedisTemplate;
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

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 全局过滤器，用于处理请求和响应的日志、鉴权、限流等功能
 */
@Slf4j
@Component
@CrossOrigin(origins = "*")
public class InterfaceInvokeFilter implements GlobalFilter, Ordered {

    @Resource
    private RedisTemplate redisTemplate;

    @DubboReference
    private InnerUserService innerUserService; // 用户服务接口

    @DubboReference
    private InnerInterfaceInfoService innerInterfaceInfoService; // 接口信息服务接口

    @DubboReference
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService; // 用户接口信息服务接口

    private static final String INTERFACE_HOST = "http://localhost:8123"; // 接口的主机地址

    User invokeUser = null;

    InterfaceInfo interfaceInfo = null;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String webPath = request.getPath().value();
        log.info("InterfaceInvokeFilter:请求路径:{}", webPath);
        // 只在路径为 /api/interfaceInvoke/** 时进行过滤
        AntPathMatcher pathMatcher = new AntPathMatcher();
        if (pathMatcher.match("/api/interfaceInvoke/extend", webPath) ||
                !pathMatcher.match("/api/interfaceInvoke/**", webPath)) {
            log.info("InterfaceInvokeFilter Skip成功");
            return chain.filter(exchange);
        }

        // 1. 请求日志记录
        String path = INTERFACE_HOST + request.getPath().value(); // 请求路径
        String method = request.getMethod().toString(); // 请求方法
        log.info("请求唯一标识：" + request.getId());
        log.info("请求路径：" + path);
        log.info("请求方法：" + method);
        log.info("请求参数：" + request.getQueryParams());
        String sourceAddress = request.getLocalAddress().getHostString(); // 本地地址
        log.info("请求来源地址：" + sourceAddress);
        log.info("请求来源地址：" + request.getRemoteAddress()); // 远程地址
        ServerHttpResponse response = exchange.getResponse();

        // 3. 用户鉴权（判断 ak、sk 是否合法）
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey"); // 获取 accessKey
        String nonce = headers.getFirst("nonce"); // 获取 nonce
        String timestamp = headers.getFirst("timestamp"); // 获取时间戳
        String sign = headers.getFirst("sign"); // 获取签名
        String body = headers.getFirst("body"); // 获取请求体

        // 从数据库中查找用户
        try {
            invokeUser = innerUserService.getInvokeUser(accessKey);
        } catch (Exception e) {
            log.error("getInvokeUser error", e);
        }

        // 如果用户不存在，返回未授权
        if (invokeUser == null) {
            return handleNoAuth(response);
        }

        // 检查 nonce 是否有效
        if (Long.parseLong(nonce) > 10000L) {
            return handleNoAuth(response);
        }

        // 检查时间戳是否超时
        Long currentTime = System.currentTimeMillis() / 1000;
        final Long FIVE_MINUTES = 60 * 5L;
        if ((currentTime - Long.parseLong(timestamp)) >= FIVE_MINUTES) {
            return handleNoAuth(response);
        }

        // 从数据库中查找用户的 secretKey
        String secretKey = invokeUser.getSecretKey();
        String serverSign = body == null ? SignUtils.genSign(secretKey) : SignUtils.genSign(body, secretKey);

        // 验证签名是否匹配
        if (sign == null || !sign.equals(serverSign)) {
            return handleNoAuth(response);
        }

        // 4. 检查请求的接口是否存在，以及请求方法是否匹配
        try {
            interfaceInfo = innerInterfaceInfoService.getInterfaceInfo(path, method);
        } catch (Exception e) {
            log.error("getInterfaceInfo error", e);
        }

        // 如果接口不存在，返回未授权
        if (interfaceInfo == null) {
            return handleNoAuth(response);
        }

        // 根据UserInterfaceInfo表检查用户是否还有剩余的调用次数
        long remainingCalls = innerUserInterfaceInfoService.getApiRemainingCalls(interfaceInfo.getId(), invokeUser.getId());
        if (remainingCalls <= 0) {
            return handleInvokeLimitError(response);
        }

        // 6.利用Redis做分布式锁，同一用户在同一时间只能调用一个接口
        String redisKey = invokeUser.getId() + ":invoke:" + interfaceInfo.getId(); // 生成 Redis Key
        Boolean b = redisTemplate.opsForValue().setIfAbsent(redisKey, String.valueOf(remainingCalls), 10, TimeUnit.SECONDS);
        if (b == null || !b) {
            return handleInvokeTooFrequentlyError(response);
        }
        log.info("redisKey:{}, value:{}", redisKey, remainingCalls);

        // 处理请求响应
        return handleResponse(exchange, chain, interfaceInfo.getId(), invokeUser.getId());
    }

    /**
     * 处理响应
     *
     * @param exchange        服务器交换
     * @param chain           过滤链
     * @param interfaceInfoId 接口信息ID
     * @param userId          用户ID
     * @return Mono<Void>
     */
    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain, long interfaceInfoId, long userId) {
        try {
            ServerHttpResponse originalResponse = exchange.getResponse(); // 获取原始响应
            DataBufferFactory bufferFactory = originalResponse.bufferFactory(); // 缓存数据的工厂
            HttpStatus statusCode = originalResponse.getStatusCode(); // 获取响应状态码

            // 只有在调用成功的时候才会进行调用次数的增加
            if (statusCode == HttpStatus.OK) {
                // 装饰响应以增强功能
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            return super.writeWith(
                                    fluxBody.handle((dataBuffer, sink) -> {
                                        String redisKey = userId + ":invoke:" + interfaceInfoId; // 生成 Redis Key
                                        log.info(redisKey);

                                        // 获取当前剩余调用次数
                                        String RemainingCalls = (String) redisTemplate.opsForValue().get(redisKey);
                                        log.info(RemainingCalls);

                                        // 尝试更新数据库
                                        if (RemainingCalls != null && Integer.parseInt(RemainingCalls) > 0) {
                                            // 更新调用次数
                                            innerUserInterfaceInfoService.invokeCount(interfaceInfoId, userId);
                                            // 删除redis键值对
                                            redisTemplate.delete(redisKey);
                                            log.info("调用次数更新成功");
                                        } else {
                                            log.error("用户 {} 调用接口 {} 时，调用次数已用尽，无法进行调用",
                                                    invokeUser.getUserName(), interfaceInfo.getName());
                                            sink.error(new RuntimeException("调用次数已用尽，无法进行调用"));
                                            return; // 直接返回
                                        }

                                        byte[] content = new byte[dataBuffer.readableByteCount()];
                                        dataBuffer.read(content);
                                        DataBufferUtils.release(dataBuffer);

                                        // 构建日志
                                        String data = new String(content, StandardCharsets.UTF_8);
                                        log.info("响应结果：{}", data);
                                        sink.next(bufferFactory.wrap(content));
                                    }));
                        } else {
                            log.error("<--- {} 响应code异常", statusCode);
                        }
                        return super.writeWith(body);
                    }
                };

                // 设置 response 对象为装饰过的
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }
            return chain.filter(exchange); // 降级处理返回数据
        } catch (Exception e) {
            log.error("网关处理响应异常", e);
            return chain.filter(exchange); // 返回原始响应
        }
    }

    @Override
    public int getOrder() {
        return -1; // 指定过滤器的执行顺序
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

    // 处理调用过于频繁的错误
    // 处理调用次数用尽的错误
    public Mono<Void> handleInvokeTooFrequentlyError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN); // 设置响应状态为403
        String message = "您的调用过于频繁，请稍后重试"; // 错误信息
        DataBuffer buffer = response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8)); // 包装错误信息
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8"); // 设置响应头
        return response.writeWith(Mono.just(buffer)); // 返回错误信息
    }
}
