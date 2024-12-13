package com.rickey.core.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author rickey
 * @create 2024-12-13 14:13
 */
@Data
public class UserDevKeyVO implements Serializable {
    private static final long serialVersionUID = 6703326011663561616L;

    private String accessKey;
    private String secretKey;

}
