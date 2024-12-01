package com.rickey.thirdParty.common;

public enum AlipayTradeStatus {
    TRADE_SUCCESS("TRADE_SUCCESS"),
    TRADE_FINISHED("TRADE_FINISHED");

    private final String status;

    AlipayTradeStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}