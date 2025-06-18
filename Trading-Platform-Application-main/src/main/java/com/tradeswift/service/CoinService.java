package com.tradeswift.service;
import com.tradeswift.models.Coin;
import org.springframework.context.annotation.Bean;

import java.util.List;


public interface CoinService {
    List<Coin> getCoinList(int page);
    String getMarketChart(String coinId,int days);
    String getCoinDetails(String coinId);
    Coin findById(String coinId);
    String searchCoin(String keywords);
    String getTop50CoinsByMarketRank();
    String getTreadingCoins();
}
