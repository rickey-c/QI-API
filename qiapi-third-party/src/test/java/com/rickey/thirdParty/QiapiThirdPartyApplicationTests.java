package com.rickey.thirdParty;

import com.rickey.thirdParty.common.AlipayTradeStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class QiapiThirdPartyApplicationTests {

    @Test
    void contextLoads() {
        String status = AlipayTradeStatus.TRADE_SUCCESS.getStatus();
        log.info("status:{}", status);
    }

}
