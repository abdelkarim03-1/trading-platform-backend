package com.tradeswift.request;

import com.tradeswift.domain.OrderType;
import lombok.Data;

@Data
public class OrderReq {
    private String coinId;
    private double quantity;
    private OrderType orderType;
}
