package com.tradeswift.service;
import com.tradeswift.domain.OrderType;
import com.tradeswift.models.Coin;
import com.tradeswift.models.Order;
import com.tradeswift.models.OrderItem;
import com.tradeswift.models.User;

import java.util.List;

public interface OrderService {

    Order createOrder(User user, OrderItem orderItem, OrderType orderType);

    Order getOrderById(Long orderId);

    List<Order> getAllOrdersOfUser(Long userId, OrderType orderType, String assetSymbol);

    Order processOrder(Coin coin, double quantity, OrderType orderType, User user) throws Exception;

}