package com.rickey.apiInterface.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.rickey.common.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Component
@Slf4j
public class BlockHandlerService {
    // 限流或者服务降级处理
    public static String NameControllerBlockHandler(@RequestBody User user, BlockException e) {
        log.info("NameController流控降级处理");
        return "Name相关接口调用过于频繁，请稍后重试";
    }

    // 限流或者服务降级处理
    public static String RandomControllerBlockHandler(BlockException e) {
        log.info("RandomController流控降级处理");
        return "Random相关接口调用过于频繁，请稍后重试";
    }
}
