package com.rickey.thirdParty.controller;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.rickey.common.common.BaseResponse;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.entity.Order;
import com.rickey.common.model.entity.UserInterfaceInfo;
import com.rickey.common.service.InnerOrderService;
import com.rickey.common.service.InnerUserInterfaceInfoService;
import com.rickey.common.utils.ResultUtils;
import com.rickey.thirdParty.common.AlipayTradeStatus;
import com.rickey.thirdParty.config.AliPayConfig;
import com.rickey.thirdParty.model.dto.AlipayNotifyParam;
import com.rickey.thirdParty.model.dto.OrderPayRequest;
import com.rickey.thirdParty.model.entity.AliPayInfo;
import com.rickey.thirdParty.service.AliPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Auther: rickey
 * @Date: 2020/11/13/21:47
 * @Description: 支付宝沙箱测试 controller
 */
@RestController
@RequestMapping("/alipay")
@Slf4j
public class PayController {
    @Resource
    private AliPayConfig alipayConfig;

    @Resource
    private AliPayService aliPayService;

    @DubboReference
    private InnerOrderService innerOrderService;

    @DubboReference
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private AliPayConfig aliPayConfig;

    private ExecutorService executorService = Executors.newFixedThreadPool(20);

    private final String DEAD_LETTER_TOPIC = "%DLQ%order-topic:sendUpdateMessage";

    /**
     * 支付宝支付 api
     *
     * @param orderPayRequest
     * @return
     * @throws AlipayApiException
     */
    @PostMapping(value = "/pay")
    public BaseResponse<String> alipay(@RequestBody OrderPayRequest orderPayRequest) throws AlipayApiException {
        AliPayInfo aliPayInfo = new AliPayInfo();
        // 商家订单号
        aliPayInfo.setOut_trade_no(orderPayRequest.getOutTradeNo());
        // 订单标题Q
        aliPayInfo.setSubject(orderPayRequest.getSubject());
        // 总金额
        aliPayInfo.setTotal_amount(orderPayRequest.getTotalAmount());
        // 订单描
        aliPayInfo.setDescription(orderPayRequest.getDescription());
        String form = aliPayService.aliPay(aliPayInfo);
        return ResultUtils.success(form);
    }


    /**
     * <pre>
     * 第一步:验证签名,签名通过后进行第二步
     * 第二步:按一下步骤进行验证
     * 1、商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
     * 2、判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
     * 3、校验通知中的seller_id（或者seller_email) 是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email），
     * 4、验证app_id是否为该商户本身。上述1、2、3、4有任何一个验证不通过，则表明本次通知是异常通知，务必忽略。
     * 在上述验证通过后商户必须根据支付宝不同类型的业务通知，正确的进行不同的业务处理，并且过滤重复的通知结果数据。
     * 在支付宝的业务通知中，只有交易通知状态为TRADE_SUCCESS或TRADE_FINISHED时，支付宝才会认定为买家付款成功。
     * </pre>
     *
     * @param request
     * @return
     */
    @PostMapping("/callback")
    @ResponseBody
    public BaseResponse<String> callback(HttpServletRequest request) {
        Map<String, String> params = convertRequestParamsToMap(request); // 将异步通知中收到的待验证所有参数都存放到map中
        String paramsJson = JSON.toJSONString(params);
        log.info("支付宝回调，{}", paramsJson);
        try {
            // 调用SDK验证签名
            boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayConfig.getPublicKey(),
                    alipayConfig.getCharset(), alipayConfig.getSignType());
            log.info("params : {}", paramsJson);
            log.info("publicKey : {}", alipayConfig.getPublicKey());
            log.info("charset : {}", alipayConfig.getCharset());
            log.info("signType : {}", alipayConfig.getSignType());
            if (signVerified) {
                log.info("支付宝回调签名认证成功");
                // 按照支付结果异步通知中的描述，对支付结果中的业务内容进行1\2\3\4二次校验，校验成功后在response中返回success，校验失败返回failure
                this.check(params);
                log.info("params参数校验成功");
                // 另起线程处理业务
                executorService.execute(() -> {
                    log.info("线程池开始执行任务");
                    AlipayNotifyParam param = buildAlipayNotifyParam(params);
                    String trade_status = param.getTradeStatus();
                    // 支付成功
                    if (trade_status.equals(AlipayTradeStatus.TRADE_SUCCESS.getStatus())
                            || trade_status.equals(AlipayTradeStatus.TRADE_FINISHED.getStatus())) {
                        // 处理支付成功逻辑
                        try {
                            String topic = "order-topic";
                            String orderId = params.get("out_trade_no");
                            log.info("orderId:{}", orderId);
                            byte[] body = orderId.getBytes(StandardCharsets.UTF_8);
                            //普通消息发送
                            rocketMQTemplate.asyncSend(topic,
                                    MessageBuilder.withPayload(body)
                                            .build(), new SendCallback() {
                                        @Override
                                        public void onSuccess(SendResult sendResult) {
                                            // 处理消息发送成功逻辑
                                            log.info("更新订单状态消息发送成功");
                                            // TODO增加接口调用次数
                                            Order order = innerOrderService.getOrderById(Long.valueOf(orderId));
                                            Long userId = order.getUserId();
                                            Long interfaceId = order.getInterfaceId();
                                            Integer increment = order.getQuantity();
                                            UserInterfaceInfo userInterfaceInfo = innerUserInterfaceInfoService.getUserInterfaceInfo(userId, interfaceId);
                                            Integer leftNum = userInterfaceInfo.getLeftNum();
                                            boolean updateLeftNum = innerUserInterfaceInfoService.updateLeftNum(interfaceId, userId, leftNum, increment);
                                            if (!updateLeftNum) {
                                                log.info("更新调用次数失败");
                                            }
                                            log.debug("更新接口调用次数成功");
                                        }

                                        @Override
                                        public void onException(Throwable throwable) {
                                            // 处理消息发送异常逻辑,加入死信队列人工处理
                                            sendToDeadLetterQueue(DEAD_LETTER_TOPIC, throwable.getMessage());
                                            log.info("更新订单状态消息发送失败");
                                            throw new RuntimeException(throwable);
                                        }
                                    });
                        } catch (Exception e) {
                            log.error("支付宝回调业务处理报错,params:{}", paramsJson, e);
                        }
                    } else {
                        log.error("没有处理支付宝回调业务，支付宝交易状态：{},params:{}", trade_status, paramsJson);
                    }
                });
                // 如果签名验证正确，立即返回success，后续业务另起线程单独处理
                // 业务处理失败，可查看日志进行补偿，跟支付宝已经没多大关系。
                ResultUtils.success("success");
            } else {
                log.info("支付宝回调签名认证失败，signVerified=false, paramsJson:{}", paramsJson);
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        } catch (AlipayApiException e) {
            log.error("支付宝回调签名认证失败,paramsJson:{},errorMsg:{}", paramsJson, e.getMessage());
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success("success");
    }

    // 将request中的参数转换成Map
    private static Map<String, String> convertRequestParamsToMap(HttpServletRequest request) {
        Map<String, String> retMap = new HashMap<String, String>();

        Set<Map.Entry<String, String[]>> entrySet = request.getParameterMap().entrySet();

        for (Map.Entry<String, String[]> entry : entrySet) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            int valLen = values.length;

            if (valLen == 1) {
                retMap.put(name, values[0]);
            } else if (valLen > 1) {
                StringBuilder sb = new StringBuilder();
                for (String val : values) {
                    sb.append(",").append(val);
                }
                retMap.put(name, sb.toString().substring(1));
            } else {
                retMap.put(name, "");
            }
        }

        return retMap;
    }

    private AlipayNotifyParam buildAlipayNotifyParam(Map<String, String> params) {
        String json = JSON.toJSONString(params);
        return JSON.parseObject(json, AlipayNotifyParam.class);
    }

    /**
     * 1、商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
     * 2、判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
     * 3、校验通知中的seller_id（或者seller_email)是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email），
     * 4、验证app_id是否为该商户本身。上述1、2、3、4有任何一个验证不通过，则表明本次通知是异常通知，务必忽略。
     * 在上述验证通过后商户必须根据支付宝不同类型的业务通知，正确的进行不同的业务处理，并且过滤重复的通知结果数据。
     * 在支付宝的业务通知中，只有交易通知状态为TRADE_SUCCESS或TRADE_FINISHED时，支付宝才会认定为买家付款成功。
     *
     * @param params
     * @throws AlipayApiException
     */
    private void check(Map<String, String> params) throws AlipayApiException {
        log.info("check方法开始");
        String outTradeNo = params.get("out_trade_no");
        log.info("orderId:{}", outTradeNo);

        // 1、商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
        Order order = innerOrderService.getOrderById(Long.valueOf(outTradeNo)); // 这个方法自己实现
        log.info("order = {}", order);
        if (order == null) {
            log.info("order is null");
            throw new AlipayApiException("out_trade_no错误");
        }

        // 2、判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
        String total_amount = params.get("total_amount");
        String totalPrice = order.getTotalPrice().toString();
        log.info("total_amount = {}", total_amount);
        if (!total_amount.equals(totalPrice)) {
            throw new AlipayApiException("error total_amount");
        }

        // 3、校验通知中的seller_id（或者seller_email)是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email），
        // 第三步可根据实际情况省略

        // 4、验证app_id是否为该商户本身。
        log.debug("app_id", params.get("app_id"));
        if (!params.get("app_id").equals(aliPayConfig.getAppId())) {
            throw new AlipayApiException("app_id不一致");
        }
    }

    private void sendToDeadLetterQueue(String topic, String message) {
        // 假设有一个死信队列服务
        rocketMQTemplate.convertAndSend(DEAD_LETTER_TOPIC, message);
    }
}
