package com.rickey.gateway.filter;

import cn.hutool.core.text.AntPathMatcher;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
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
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.servlet.annotation.WebFilter;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

/**
 * 过滤器，重置redis中session有效期
 */
@Component
@WebFilter(urlPatterns = "/*", filterName = "sessionExpireFilter")
@Slf4j
@CrossOrigin(origins = "*")
public class SessionExpireFilter implements GlobalFilter, Ordered {

    private final RedisUtil redisUtil;

    private final static String COOKIE_NAME = "rickey_login_token";

    @Autowired
    public SessionExpireFilter(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String webPath = request.getPath().value();
        log.info("SessionExpireFilter: 请求路径: {}", webPath);

        List<String> skipPaths = Arrays.asList(
                "/api/user/register",
                "/api/user/login",
                "/api/user/email",
                "/api/interfaceInvoke/**",
                "/api/interfaceInfo/sdk"
        );

        AntPathMatcher pathMatcher = new AntPathMatcher();
        if (skipPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, webPath))) {
            log.info("SessionExpireFilter: 跳过路径: {}", webPath);
            return chain.filter(exchange);
        }

        InetAddress remoteAddr = request.getRemoteAddress().getAddress();
        HttpCookie token = request.getCookies().getFirst(COOKIE_NAME);

        // 校验 IP 和 Token
        if ("127.0.0.1".equals(remoteAddr.getHostAddress())) {
            return handleTokenValidation(exchange, chain, token);
        } else {
            return handleRateLimitAndTokenValidation(exchange, chain, token, remoteAddr);
        }
    }

    private Mono<Void> handleTokenValidation(ServerWebExchange exchange, GatewayFilterChain chain, HttpCookie token) {
        if (token == null || StrUtil.isBlank(token.getValue())) {
            log.warn("Token 为空，拒绝请求。");
            return unauthorizedResponse(exchange);
        }

        String loginToken = token.getValue();
        User user = getUserByToken(loginToken);
        if (user == null) {
            log.warn("无效的 Token 或用户信息已过期，Token: {}", loginToken);
            clearInvalidToken(exchange);
            return unauthorizedResponse(exchange);
        }

        log.info("Token 验证成功，刷新 Redis 过期时间。");
        refreshTokenExpire(user, loginToken);
        return forwardRequestWithUser(exchange, chain, user);
    }

    private Mono<Void> handleRateLimitAndTokenValidation(ServerWebExchange exchange, GatewayFilterChain chain, HttpCookie token, InetAddress remoteAddr) {
        Entry entry = null;
        try {
            entry = SphU.entry("IP-Rule", EntryType.IN, 1, remoteAddr);
            return handleTokenValidation(exchange, chain, token);
        } catch (BlockException ex) {
            log.warn("限流触发，IP: {}", remoteAddr.getHostAddress());
            return rateLimitResponse(exchange);
        } catch (Exception ex) {
            log.error("处理请求时发生异常: ", ex);
            return errorResponse(exchange);
        } finally {
            if (entry != null) {
                entry.exit(1, remoteAddr);
            }
        }
    }

    private void refreshTokenExpire(User user, String loginToken) {
        redisUtil.expire("session:" + loginToken, 600);
        redisUtil.expire("token:user:" + user.getId(), 600);
    }

    private Mono<Void> forwardRequestWithUser(ServerWebExchange exchange, GatewayFilterChain chain, User user) {
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("userId", String.valueOf(user.getId()))
                .header("userName", user.getUserName())
                .header("accessKey", user.getAccessKey())
                .header("secretKey", user.getSecretKey())
                .header("userRole", user.getUserRole())
                .build();
        log.info("请求头已添加用户信息，转发请求。");
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private void clearInvalidToken(ServerWebExchange exchange) {
        exchange.getResponse().addCookie(ResponseCookie.from(COOKIE_NAME, null)
                .path("/")
                .maxAge(0)
                .build());
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> rateLimitResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> errorResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return exchange.getResponse().setComplete();
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
