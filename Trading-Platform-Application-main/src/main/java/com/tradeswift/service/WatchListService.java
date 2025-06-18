package com.tradeswift.service;

import com.tradeswift.models.Coin;
import com.tradeswift.models.User;
import com.tradeswift.models.WatchList;

public interface WatchListService {

    WatchList findUserWatchList(Long userId);
    WatchList createWatchList(User user);
    WatchList findById(Long id);
    Coin addItemToWatchList(Coin coin,User user);

}
