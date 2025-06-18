package com.tradeswift.service;

import com.tradeswift.models.Asset;
import com.tradeswift.models.Coin;
import com.tradeswift.models.User;

import java.util.List;

public interface AssetService {

    Asset createAsset(User user, Coin coin, double quantity);
    Asset getAssetById(Long assetId);
    Asset getAssetByUserIdAndId(Long userId,Long assetId);
    List<Asset> getUserAsset(Long userId);
    Asset updateAsset(Long assetId,double quantity);
    Asset findAssetByUserIdAndCoinId(Long userId,String coinId);
    void deleteAsset(Long assetId);

}
