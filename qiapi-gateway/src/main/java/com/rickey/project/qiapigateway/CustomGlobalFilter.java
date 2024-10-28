package com.rickey.project.qiapigateway;

import com.rickey.qiapiclientsdk.utils.SignUtils;
import com.rickey.qiapicommon.model.entity.InterfaceInfo;
import com.rickey.qiapicommon.model.entity.User;
import com.rickey.qiapicommon.service.InnerInterfaceInfoService;
import com.rickey.qiapicommon.service.InnerUserInterfaceInfoService;
import com.rickey.qiapicommon.service.InnerUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局过滤器，用于处理请求和响应的日志、鉴权、限流等功能
 */
@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    @DubboReference
    private InnerUserService innerUserService; // 用户服务接口

    @DubboReference
    private InnerInterfaceInfoService innerInterfaceInfoService; // 接口信息服务接口

    @DubboReference
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService; // 用户接口信息服务接口

    // TODO: IP白名单功能待实现
    private static final List<String> IP_WHITE_LIST = null;

    private static final String INTERFACE_HOST = "http://localhost:8123"; // 接口的主机地址

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 请求日志记录
        ServerHttpRequest request = exchange.getRequest();
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

        // TODO 2. 访问控制 - 黑白名单
//        if (!IP_WHITE_LIST.contains(sourceAddress)) {
//            response.setStatusCode(HttpStatus.FORBIDDEN); // 如果不在白名单中，拒绝访问
//            return response.setComplete();
//        }

        // 3. 用户鉴权（判断 ak、sk 是否合法）
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey"); // 获取 accessKey
        String nonce = headers.getFirst("nonce"); // 获取 nonce
        String timestamp = headers.getFirst("timestamp"); // 获取时间戳
        String sign = headers.getFirst("sign"); // 获取签名
        String body = headers.getFirst("body"); // 获取请求体

        // 从数据库中查找用户
        User invokeUser = null;
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
        String serverSign = null;
        if (body == null) {
            serverSign = SignUtils.genSign(secretKey); // 生成签名
        } else {
            serverSign = SignUtils.genSign(body, secretKey); // 生成签名
        }

        // 验证签名是否匹配
        if (sign == null || !sign.equals(serverSign)) {
            return handleNoAuth(response);
        }

        // 4. 检查请求的接口是否存在，以及请求方法是否匹配
        InterfaceInfo interfaceInfo = null;
        try {
            interfaceInfo = innerInterfaceInfoService.getInterfaceInfo(path, method);
        } catch (Exception e) {
            log.error("getInterfaceInfo error", e);
        }

        // 如果接口不存在，返回未授权
        if (interfaceInfo == null) {
            return handleNoAuth(response);
        }

        // 5. 检查用户是否还有剩余的调用次数,保留,如果一开始就没有调用次数,那直接拦截
        // TODO 利用事务消息解决一致性问题，这段逻辑删除
        boolean hasRemainingInvokeCount = false;
        try {
            hasRemainingInvokeCount = innerUserInterfaceInfoService
                    .hasRemainingInvokeCount(interfaceInfo.getId(), invokeUser.getId());
        } catch (Exception e) {
            log.error("hasRemainingInvokeCount error", e);
        }
        // 如果调用次数用尽，返回调用限制错误
        if (!hasRemainingInvokeCount) {
            return handleInvokeLimitError(response);
        }

        // 处理请求响应
        return handleResponse(exchange, chain, interfaceInfo.getId(), invokeUser.getId());
    }

    /**
     * 处理响应
     *
     * @param exchange 服务器交换
     * @param chain 过滤链
     * @param interfaceInfoId 接口信息ID
     * @param userId 用户ID
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
                        log.info("body instanceof Flux: {}", (body instanceof Flux));
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            return super.writeWith(
                                    fluxBody.map(dataBuffer -> {
                                        // 7. 调用成功，接口调用次数 + 1 invokeCount
                                        //TODO 通过分布式消息实现事务之后删除这段逻辑
                                        try {
                                            innerUserInterfaceInfoService.invokeCount(interfaceInfoId, userId);
                                        } catch (Exception e) {
                                            log.error("invokeCount error", e);
                                        }
                                        byte[] content = new byte[dataBuffer.readableByteCount()]; // 读取响应内容
                                        dataBuffer.read(content); // 读取内容
                                        DataBufferUtils.release(dataBuffer); // 释放内存

                                        // 构建日志
                                        StringBuilder sb2 = new StringBuilder(200);
                                        List<Object> rspArgs = new ArrayList<>();
                                        rspArgs.add(originalResponse.getStatusCode());
                                        String data = new String(content, StandardCharsets.UTF_8); // 转换为字符串
                                        sb2.append(data);
                                        // 打印日志
                                        log.info("响应结果：" + data);
                                        return bufferFactory.wrap(content); // 返回响应
                                    }));
                        } else {
                            // 8. 调用失败，记录异常
                            log.error("<--- {} 响应code异常", getStatusCode());
                        }
                        return super.writeWith(body); // 默认行为
                    }
                };
                // 设置 response 对象为装饰过的
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }
            return chain.filter(exchange); // 降级处理返回数据
        } catch (Exception e) {
            log.error("网关处理响应异常" + e);
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
}
