package com.rickey.clientSDK.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * IP 分析参数类，用于封装 IP 分析请求所需的参数。
 *
 * 该类包含以下字段：
 * <ul>
 *     <li>ip：目标 IP 地址，不能为空</li>
 * </ul>
 *
 * @author rickey-c
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IpAnaParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标 IP 地址
     * <p>
     * 该字段不能为空。
     */
    @NotNull(message = "参数不能为空")
    private String ip;
}
