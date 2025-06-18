package com.tradeswift.service.implement;
import com.tradeswift.domain.OrderStatus;
import com.tradeswift.domain.OrderType;
import com.tradeswift.exception.ResourceNotFoundException;
import com.tradeswift.models.*;
import com.tradeswift.repositories.OrderItemRepository;
import com.tradeswift.repositories.OrderRepository;
import com.tradeswift.service.AssetService;
import com.tradeswift.service.OrderService;
import com.tradeswift.service.WalletService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImple implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private AssetService assetService;
    
    @Autowired
    private com.tradeswift.repositories.TransactionHistoryRepository transactionHistoryRepository;

    @Override
    public Order createOrder(User user, OrderItem orderItem, OrderType orderType) {
        double price = orderItem.getCoin().getCurrentPrice() * orderItem.getQuatily();

        Order order = new Order();
        order.setUser(user);
        order.setOrderItem(orderItem);
        order.setOrderType(orderType);
        order.setPrice(BigDecimal.valueOf(price));
        order.setOrderStatus(OrderStatus.PENDING);
        order.setTimestamp(LocalDateTime.now());

        return this.orderRepository.save(order);
    }

    @Override
    public Order getOrderById(Long orderId) {
        return this.orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("order", "id", String.valueOf(orderId)));
    }

    @Override
    public List<Order> getAllOrdersOfUser(Long userId, OrderType orderType, String assetSymbol) {
        return this.orderRepository.findByUserId(userId);
    }

    private OrderItem createOrderItem(Coin coin, double quantity, double buyPrice, double sellPrice) {

        OrderItem orderItem = new OrderItem();

        orderItem.setQuatily(quantity);
        orderItem.setCoin(coin);
        orderItem.setSellPrice(sellPrice);
        orderItem.setBuyPrice(buyPrice);

        return this.orderItemRepository.save(orderItem);
    }

    @Transactional
    public Order buyAsset(Coin coin, double quantity, User user) throws Exception {
        if (quantity <= 0) {
            throw new Exception("Quantity should be >0");
        }
        double buyPrice = coin.getCurrentPrice();

        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, 0);
        Order order = createOrder(user, orderItem, OrderType.BUY);

        orderItem.setOrder(order);

        this.walletService.payOrderPayment(order, user);

        order.setOrderStatus(OrderStatus.SUCCESS);
        order.setOrderType(OrderType.BUY);

        Asset oldAsset = this.assetService.findAssetByUserIdAndCoinId(order.getUser().getId(), order.getOrderItem().getCoin().getId());

        if (oldAsset == null) {
            this.assetService.createAsset(user, coin, quantity);
        } else {
            this.assetService.updateAsset(oldAsset.getId(), quantity);
        }
        
        // Create transaction record for buying asset
        try {
            Wallet wallet = this.walletService.getUserWallet(user);
            com.tradeswift.models.WalletTransaction walletTransaction = new com.tradeswift.models.WalletTransaction();
            walletTransaction.setWallet(wallet);
            walletTransaction.setAmount(order.getPrice().longValue());
            walletTransaction.setDate(java.time.LocalDate.now());
            walletTransaction.setWalletTransactionType(com.tradeswift.domain.WalletTransactionType.BUY_ASSET);
            String formattedQuantity = String.format("%.8f", quantity);
            walletTransaction.setPurpose("Buy " + quantity + " " + coin.getSymbol() + " at " + coin.getCurrentPrice());
            this.transactionHistoryRepository.save(walletTransaction);
        } catch (Exception e) {
            System.err.println("Error creating transaction record: " + e.getMessage());
        }
        
        return this.orderRepository.save(order);

    }

    @Transactional
    public Order sellAsset(Coin coin, double quantity, User user) throws Exception {

        if (quantity < 0) {
            throw new Exception("Quantity should be >0");
        }
        double sellPrice = coin.getCurrentPrice();
        Asset assetToSell = this.assetService.findAssetByUserIdAndCoinId(user.getId(), coin.getId());
        double buyPrice = assetToSell.getBuyPrice();

        if (assetToSell != null) {
            OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, sellPrice);
            Order order = createOrder(user, orderItem, OrderType.SELL);
            orderItem.setOrder(order);

            if (assetToSell.getQuantity() >= quantity) {
                order.setOrderStatus(OrderStatus.SUCCESS);
                order.setOrderType(OrderType.SELL);
                Order savedOrder = this.orderRepository.save(order);

                this.walletService.payOrderPayment(order, user);

                Asset updatedAsset = this.assetService.updateAsset(assetToSell.getId(), -quantity);

                if (updatedAsset.getQuantity() * coin.getCurrentPrice() <= 1) {
                    assetService.deleteAsset(updatedAsset.getId());
                }
                
                // Create transaction record for selling asset
                try {
                    Wallet wallet = this.walletService.getUserWallet(user);
                    com.tradeswift.models.WalletTransaction walletTransaction = new com.tradeswift.models.WalletTransaction();
                    walletTransaction.setWallet(wallet);
                    walletTransaction.setAmount(order.getPrice().longValue());
                    walletTransaction.setDate(java.time.LocalDate.now());
                    walletTransaction.setWalletTransactionType(com.tradeswift.domain.WalletTransactionType.SELL_ASSET);
                    String formattedQuantity = String.format("%.8f", quantity);
                    walletTransaction.setPurpose("Sell " + formattedQuantity + " " + coin.getSymbol() + " at " + coin.getCurrentPrice());
                    this.transactionHistoryRepository.save(walletTransaction);
                } catch (Exception e) {
                    System.err.println("Error creating transaction record: " + e.getMessage());
                }
                
                return savedOrder;
            }
            throw new Exception("Insufficient Quantity to sell");
        }
        throw new Exception("asset not found !");
}

    @Override
    @Transactional
    public Order processOrder(Coin coin, double quantity, OrderType orderType, User user) throws Exception {

        if(orderType.equals(OrderType.BUY)){
            return  buyAsset(coin,quantity,user);
        }
        else if(orderType==OrderType.SELL)
        {
            return  sellAsset(coin,quantity,user);
        }
        throw  new Exception("invalid Order Type");
    }
}
