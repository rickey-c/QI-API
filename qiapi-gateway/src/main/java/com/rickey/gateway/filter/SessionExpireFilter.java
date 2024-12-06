package com.rickey.gateway.filter;

import cn.hutool.core.text.AntPathMatcher;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.entity.User;
import com.rickey.gateway.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.servlet.annotation.WebFilter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 过滤器，重置redis中session有效期
 */
@Component
@WebFilter(urlPatterns = "/*", filterName = "sessionExporeFilter")
@Slf4j
@CrossOrigin(origins = "*")
public class SessionExpireFilter implements GlobalFilter, Ordered {

    private final RedisUtil redisUtil;

    private final static String COOKIE_NAME = "rickey_login_token";

    @Autowired
    public SessionExpireFilter(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * Process the Web request and (optionally) delegate to the next {@code WebFilter}
     * through the given {@link GatewayFilterChain}.
     *
     * @param exchange the current server exchange
     * @param chain    provides a way to delegate to the next handleResponse
     * @return {@code Mono<Void>} to indicate when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String webPath = request.getPath().value();
        log.info("SessionExpireFilter:请求路径:{}", webPath);
        List<String> skipPaths = Arrays.asList(
                "/api/user/login",
                "/api/interfaceInvoke/**"
        );

        // 跳过特定路径
        AntPathMatcher pathMatcher = new AntPathMatcher();
        // 使用 Stream 判断是否匹配任何路径
        if (skipPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, webPath))) {
            log.info("SessionExpireFilter Skip成功: {}", webPath);
            return chain.filter(exchange);
        }

        // 从请求中获取 Cookies
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        HttpCookie token = cookies.getFirst(COOKIE_NAME);

        if (token != null) {
            String loginToken = token.getValue();
            if (StrUtil.isNotBlank(loginToken)) {
                // 从 Redis 中获取用户数据
                User user = getUserByToken(loginToken);
                if (user != null) {
                    log.info("token验证成功，正在刷新expire时间...");
                    // 重置 Redis 中的过期时间
                    redisUtil.expire("session:" + loginToken, 600);
                    redisUtil.expire("token:user:" + user.getId(), 600);

                    // 将用户信息放入请求头中
                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .header("userId", String.valueOf(user.getId())) // 添加用户ID到Header
                            .header("userName", user.getUserName())// 添加用户名到Header（可以添加更多用户信息）
                            .header("accessKey", user.getAccessKey())
                            .header("secretKey", user.getSecretKey())
                            .header("userRole", user.getUserRole())
                            .build();

                    // 使用修改后的请求继续链式调用
                    log.info("刷新expire时间成功，转发请求");
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                } else {
                    log.warn("无效的 Token 或用户信息已过期，Token: {}", loginToken);

                    // 清除无效 Token（从 Cookie 中删除）
                    exchange.getResponse().addCookie(ResponseCookie.from(COOKIE_NAME, null)
                            .path("/")
                            .maxAge(0)
                            .build());
                    // 返回未登录响应
                    return unauthorizedResponse(exchange);
                }
            }
        }

        // 如果没有 Token，直接返回未登录
        return unauthorizedResponse(exchange);
    }


    /**
     * 构造未登录的响应
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        // 设置响应体为简单文本
        String responseBody = "Unauthorized: 用户未登录，请重新登录";

        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }


    public User getUserByToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 查询 Redis 中的用户数据
        String userJson = (String) redisUtil.get("session:" + token);
        System.out.println("session:" + token);
        if (StringUtils.isBlank(userJson)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return JSONUtil.toBean(userJson, User.class);
    }


    @Override
    public int getOrder() {
        return 1; // 指定过滤器的执行顺序
    }

}
