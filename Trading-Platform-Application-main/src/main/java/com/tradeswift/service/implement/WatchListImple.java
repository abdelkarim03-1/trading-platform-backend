package com.tradeswift.service.implement;

import com.tradeswift.exception.ResourceNotFoundException;
import com.tradeswift.models.Coin;
import com.tradeswift.models.User;
import com.tradeswift.models.WatchList;
import com.tradeswift.repositories.WatchListRepository;
import com.tradeswift.service.WatchListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WatchListImple implements WatchListService {

    @Autowired
    private WatchListRepository watchListRepository;


    @Override
    public WatchList findUserWatchList(Long userId) {
        return this.watchListRepository.findByUserId(userId);
    }

    @Override
    public WatchList createWatchList(User user) {

        WatchList watchList = new WatchList();
        watchList.setUser(user);
        return this.watchListRepository.save(watchList);
    }

    @Override
    public WatchList findById(Long id) {
        return this.watchListRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("WatchList","id",String.valueOf(id)));
    }

    @Override
    public Coin addItemToWatchList(Coin coin, User user) {
        WatchList watchList = this.findUserWatchList(user.getId());

        if(watchList.getCoins().contains(coin)){
            watchList.getCoins().remove(coin);
        }
        else{
            watchList.getCoins().add(coin);
        }

        this.watchListRepository.save(watchList);
        return coin;
    }
}
