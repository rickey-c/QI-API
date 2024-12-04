package com.rickey.apiInterface.sentinel;

import com.rickey.clientSDK.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@Component
public class FallbackService {

    // 接口出现异常处理
    public static String NameControllerFallback(@RequestBody User user, Throwable e) {
        log.error("Name接口异常降级处理");
        if (e instanceof RuntimeException) {
            log.error("Name接口异常 : {}", e.getMessage());
        }
        return "Name接口出现异常,请稍后调用";
    }

    // 接口出现异常处理
    public static String RandomControllerFallback(Throwable e) {
        log.error("Random接口异常降级处理");
        if (e instanceof RuntimeException) {
            log.error("Random接口异常 : {}", e.getMessage());
        }
        return "Random接口出现异常,请稍后调用";
    }

}
