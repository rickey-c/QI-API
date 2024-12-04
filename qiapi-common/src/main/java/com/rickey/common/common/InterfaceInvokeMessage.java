package com.rickey.common.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Data
@Slf4j
@AllArgsConstructor
public class InterfaceInvokeMessage implements Serializable {
    private long interfaceInfoId;
    private long userId;
}
