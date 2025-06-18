package com.tradeswift.service.implement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeswift.exception.ResourceNotFoundException;
import com.tradeswift.models.Coin;
import com.tradeswift.repositories.CoinRepository;
import com.tradeswift.service.CoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import java.text.SimpleDateFormat;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CoinServiceImple implements CoinService {

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Binance API base URL
    private static final String BINANCE_API_BASE_URL = "https://api.binance.com";
    private static final String COINGECKO_API_BASE_URL = "https://api.coingecko.com/api/v3";


    @Override
    public List<Coin> getCoinList(int page) {
        int perPage = 10;
        int offset = (page - 1) * perPage;

        String url = BINANCE_API_BASE_URL + "/api/v3/ticker/24hr";
        RestTemplate template = new RestTemplate();

        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, entity, String.class);

            List<Map<String, Object>> allCoins = objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});

            // Filter only USDT trading pairs and sort by volume
            List<Map<String, Object>> usdtPairs = allCoins.stream()
                    .filter(coin -> ((String)coin.get("symbol")).endsWith("USDT"))
                    .sorted((a, b) -> {
                        double volumeA = Double.parseDouble((String)a.get("quoteVolume"));
                        double volumeB = Double.parseDouble((String)b.get("quoteVolume"));
                        return Double.compare(volumeB, volumeA); // Sort by volume descending
                    })
                    .collect(Collectors.toList());

            // Paginate results
            List<Map<String, Object>> paginatedCoins = usdtPairs.stream()
                    .skip(offset)
                    .limit(perPage)
                    .collect(Collectors.toList());

            // Convert to our Coin model
            List<Coin> coins = new ArrayList<>();
            int rank = offset + 1;

            for (Map<String, Object> coinData : paginatedCoins) {
                Coin coin = mapBinanceCoinData(coinData, rank++);
                coins.add(coin);

                // Save to repository for future reference
                try {
                    coinRepository.save(coin);
                } catch (Exception e) {
                    // Log but continue if saving fails
                    System.out.println("Failed to save coin: " + e.getMessage());
                }
            }

            return coins;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching coin data from Binance: " + e.getMessage(), e);
        }
    }

    @Override
    public String getMarketChart(String coinId, int days) {
        try {
            // For simplicity, we'll use symbol directly (e.g., "BTCUSDT")
            String symbol = getSymbolFromCoinId(coinId);

            // Log the symbol for debugging
            System.out.println("Fetching chart data for symbol: " + symbol);

            String interval;

            // Convert days to appropriate interval
            if (days <= 1) {
                interval = "1h"; // 1-hour intervals for 1 day
            } else if (days <= 7) {
                interval = "4h"; // 4-hour intervals for 7 days
            } else if (days <= 30) {
                interval = "1d"; // 1-day intervals for 30 days
            } else if (days <= 90) {
                interval = "3d"; // 3-day intervals for 90 days
            } else {
                interval = "1w"; // 1-week intervals for longer periods
            }

            String url = BINANCE_API_BASE_URL + "/api/v3/klines?symbol=" + symbol + "&interval=" + interval + "&limit=500";

            RestTemplate template = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

            // Try to get data from Binance
            ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, entity, String.class);

            // Process klines data to match expected format
            List<List<Object>> klines = objectMapper.readValue(response.getBody(), new TypeReference<List<List<Object>>>() {});

            Map<String, List<List<Object>>> result = new HashMap<>();
            List<List<Object>> prices = new ArrayList<>();

            for (List<Object> kline : klines) {
                Long timestamp = Long.parseLong(kline.get(0).toString());
                Double closePrice = Double.parseDouble(kline.get(4).toString());
                prices.add(Arrays.asList(timestamp, closePrice));
            }

            result.put("prices", prices);

            return objectMapper.writeValueAsString(result);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                // If the coin doesn't exist, return empty data in the expected format
                System.err.println("Error with symbol for coinId " + coinId + ": " + e.getMessage());
                Map<String, List<List<Object>>> emptyResult = new HashMap<>();
                emptyResult.put("prices", new ArrayList<>());
                try {
                    return objectMapper.writeValueAsString(emptyResult);
                } catch (Exception ex) {
                    throw new RuntimeException("Error creating empty chart result", ex);
                }
            }
            throw new RuntimeException("Error fetching market chart from Binance: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("General error fetching chart for " + coinId + ": " + e.getMessage());
            // Return empty data structure instead of throwing exception
            Map<String, List<List<Object>>> emptyResult = new HashMap<>();
            emptyResult.put("prices", new ArrayList<>());
            try {
                return objectMapper.writeValueAsString(emptyResult);
            } catch (Exception ex) {
                throw new RuntimeException("Error creating empty chart result", ex);
            }
        }
    }

    @Override
    public String getCoinDetails(String coinId) {
        // First check if we have this coin in our database
        Optional<Coin> existingCoin = coinRepository.findById(coinId);

        // Try to get more complete data from CoinGecko
        try {
            // Map your coinId to CoinGecko format
            String coingeckoId = mapToCoinGeckoId(coinId);

            String detailsUrl = COINGECKO_API_BASE_URL + "/coins/" + coingeckoId +
                    "?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false";

            RestTemplate template = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

            ResponseEntity<String> response = template.exchange(detailsUrl, HttpMethod.GET, entity, String.class);

            // Parse the CoinGecko response
            JsonNode coinGeckoData = objectMapper.readTree(response.getBody());

            // If we have an existing coin, enhance it with CoinGecko data
            Coin coin;
            if (existingCoin.isPresent()) {
                coin = existingCoin.get();

                // Preserve important fields from our existing data
                double currentPrice = coin.getCurrentPrice();
                long marketCap = coin.getMarketCap();

                // Update with CoinGecko data
                updateCoinWithCoinGeckoData(coin, coinGeckoData);

                // If CoinGecko is missing critical data, use our existing values
                if (coin.getCurrentPrice() == 0) {
                    coin.setCurrentPrice(currentPrice);
                }
                if (coin.getMarketCap() == 0) {
                    coin.setMarketCap(marketCap);
                }
            } else {
                // Create a new coin from CoinGecko data
                coin = createCoinFromCoinGeckoData(coinGeckoData);

                // If coin still has missing data, try to supplement from Binance
                if (coin.getCurrentPrice() == 0) {
                    try {
                        String binanceData = getBinanceCoinDetails(coinId);
                        Coin binanceCoin = objectMapper.readValue(binanceData, Coin.class);
                        coin.setCurrentPrice(binanceCoin.getCurrentPrice());
                        coin.setMarketCap(binanceCoin.getMarketCap());
                        // Add other critical fields here
                    } catch (Exception e) {
                        System.out.println("Could not supplement with Binance data: " + e.getMessage());
                    }
                }
            }

            // Save the enhanced coin to the repository
            coinRepository.save(coin);

            return objectMapper.writeValueAsString(coin);
        } catch (Exception e) {
            System.err.println("Error fetching from CoinGecko: " + e.getMessage());

            // Fall back to Binance if CoinGecko fails
            if (existingCoin.isPresent()) {
                try {
                    return objectMapper.writeValueAsString(existingCoin.get());
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException("Error serializing coin data", ex);
                }
            }

            // If we don't have the coin, fetch from Binance
            return getBinanceCoinDetails(coinId);
        }
    }

    private String mapToCoinGeckoId(String coinId) {
        Map<String, String> idMapping = new HashMap<>();
        idMapping.put("bitcoin", "bitcoin");
        idMapping.put("btc", "bitcoin");
        idMapping.put("ethereum", "ethereum");
        idMapping.put("eth", "ethereum");
        idMapping.put("binancecoin", "binance-coin");
        idMapping.put("bnb", "binance-coin");
        idMapping.put("ripple", "xrp");
        idMapping.put("xrp", "xrp");
        idMapping.put("cardano", "cardano");
        idMapping.put("ada", "cardano");
        idMapping.put("solana", "solana");
        idMapping.put("sol", "solana");
        // Add more mappings as needed

        String lowerCoinId = coinId.toLowerCase();
        return idMapping.getOrDefault(lowerCoinId, lowerCoinId);
    }

    // Helper to update a coin with CoinGecko data
    private void updateCoinWithCoinGeckoData(Coin coin, JsonNode data) {
        JsonNode marketData = data.get("market_data");
        if (marketData != null) {
            // Update current price if available
            if (marketData.has("current_price") && marketData.get("current_price").has("usd")) {
                coin.setCurrentPrice(marketData.get("current_price").get("usd").asDouble());
            }

            // Update market cap
            if (marketData.has("market_cap") && marketData.get("market_cap").has("usd")) {
                coin.setMarketCap(marketData.get("market_cap").get("usd").asLong());
            }

            // Update supply information
            if (marketData.has("circulating_supply") && !marketData.get("circulating_supply").isNull()) {
                coin.setCirculatingSupply(marketData.get("circulating_supply").asLong());
            }

            if (marketData.has("total_supply") && !marketData.get("total_supply").isNull()) {
                coin.setTotalSupply(marketData.get("total_supply").asLong());
            }

            if (marketData.has("max_supply") && !marketData.get("max_supply").isNull()) {
                coin.setMaxSupply(marketData.get("max_supply").asLong());
            }

            // Update all-time high/low
            if (marketData.has("ath") && marketData.get("ath").has("usd")) {
                coin.setAth(marketData.get("ath").get("usd").asDouble());
            }

            if (marketData.has("atl") && marketData.get("atl").has("usd")) {
                coin.setAtl(marketData.get("atl").get("usd").asDouble());
            }

            // Update ATH date
            if (marketData.has("ath_date") && marketData.get("ath_date").has("usd")) {
                try {
                    String athDateStr = marketData.get("ath_date").get("usd").asText();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    Date athDate = format.parse(athDateStr);
                    coin.setAthDate(athDate);
                } catch (Exception e) {
                    System.out.println("Error parsing ATH date: " + e.getMessage());
                }
            }

            // Update ATL date
            if (marketData.has("atl_date") && marketData.get("atl_date").has("usd")) {
                try {
                    String atlDateStr = marketData.get("atl_date").get("usd").asText();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    Date atlDate = format.parse(atlDateStr);
                    coin.setAtlDate(atlDate);
                } catch (Exception e) {
                    System.out.println("Error parsing ATL date: " + e.getMessage());
                }
            }

            // Update 24h data
            if (marketData.has("price_change_24h")) {
                coin.setPriceChange24h(marketData.get("price_change_24h").asDouble());
            }

            if (marketData.has("price_change_percentage_24h")) {
                coin.setPriceChangePercentage24h(marketData.get("price_change_percentage_24h").asDouble());
            }

            if (marketData.has("high_24h") && marketData.get("high_24h").has("usd")) {
                coin.setHigh24h(marketData.get("high_24h").get("usd").asDouble());
            }

            if (marketData.has("low_24h") && marketData.get("low_24h").has("usd")) {
                coin.setLow24h(marketData.get("low_24h").get("usd").asDouble());
            }
        }

        // Update image URL if available
        if (data.has("image") && data.get("image").has("large")) {
            coin.setImage(data.get("image").get("large").asText());
        }

        // Update last update timestamp
        coin.setLastUpdate(new Date());
    }

    // Helper to create a new coin from CoinGecko data
    private Coin createCoinFromCoinGeckoData(JsonNode data) {
        Coin coin = new Coin();

        // Set basic information
        coin.setId(data.get("id").asText());
        coin.setSymbol(data.get("symbol").asText().toLowerCase());
        coin.setName(data.get("name").asText());

        // Set market cap rank
        if (data.has("market_cap_rank") && !data.get("market_cap_rank").isNull()) {
            coin.setMarketCapRank(data.get("market_cap_rank").asLong());
        }

        // Update the rest of the fields
        updateCoinWithCoinGeckoData(coin, data);

        return coin;
    }

    // Original Binance implementation as fallback
    private String getBinanceCoinDetails(String coinId) {
        // Your existing implementation
        String symbol = getSymbolFromCoinId(coinId);
        String tickerUrl = BINANCE_API_BASE_URL + "/api/v3/ticker/24hr?symbol=" + symbol;
        RestTemplate template = new RestTemplate();

        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            ResponseEntity<String> response = template.exchange(tickerUrl, HttpMethod.GET, entity, String.class);

            Map<String, Object> coinData = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            Coin coin = mapBinanceCoinData(coinData, 0);

            // Save to repository
            coinRepository.save(coin);

            return objectMapper.writeValueAsString(coin);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching coin details from Binance: " + e.getMessage(), e);
        }
    }

    @Override
    public Coin findById(String coinId) {
        // First check if we have this coin in our database
        Optional<Coin> existingCoin = coinRepository.findById(coinId);

        if (existingCoin.isPresent()) {
            return existingCoin.get();
        }

        // Otherwise fetch from Binance
        String symbol = getSymbolFromCoinId(coinId);
        String tickerUrl = BINANCE_API_BASE_URL + "/api/v3/ticker/24hr?symbol=" + symbol;
        RestTemplate template = new RestTemplate();

        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            ResponseEntity<String> response = template.exchange(tickerUrl, HttpMethod.GET, entity, String.class);

            Map<String, Object> coinData = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            Coin coin = mapBinanceCoinData(coinData, 0);

            // Save to repository
            coinRepository.save(coin);

            return coin;
        } catch (Exception e) {
            throw new ResourceNotFoundException("coin", "coinId", coinId);
        }
    }

    @Override
    public String searchCoin(String keywords) {
        String exchangeInfoUrl = BINANCE_API_BASE_URL + "/api/v3/exchangeInfo";
        RestTemplate template = new RestTemplate();

        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            ResponseEntity<String> response = template.exchange(exchangeInfoUrl, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode symbols = root.get("symbols");

            List<Map<String, Object>> results = new ArrayList<>();

            for (JsonNode symbolNode : symbols) {
                String symbol = symbolNode.get("symbol").asText();
                String baseAsset = symbolNode.get("baseAsset").asText();

                // Only include USDT pairs and match search terms
                if (symbol.endsWith("USDT") &&
                        (symbol.toLowerCase().contains(keywords.toLowerCase()) ||
                                baseAsset.toLowerCase().contains(keywords.toLowerCase()))) {

                    // Fetch 24hr ticker data for this symbol
                    String tickerUrl = BINANCE_API_BASE_URL + "/api/v3/ticker/24hr?symbol=" + symbol;
                    ResponseEntity<String> tickerResponse = template.exchange(tickerUrl, HttpMethod.GET, entity, String.class);
                    Map<String, Object> tickerData = objectMapper.readValue(tickerResponse.getBody(), new TypeReference<Map<String, Object>>() {});

                    Map<String, Object> coinResult = new HashMap<>();
                    coinResult.put("id", baseAsset.toLowerCase());
                    coinResult.put("symbol", baseAsset.toLowerCase());
                    coinResult.put("name", baseAsset);
                    coinResult.put("current_price", Double.parseDouble(tickerData.get("lastPrice").toString()));

                    results.add(coinResult);

                    // Limit to 10 results for performance
                    if (results.size() >= 10) {
                        break;
                    }
                }
            }

            Map<String, Object> searchResults = new HashMap<>();
            searchResults.put("coins", results);

            return objectMapper.writeValueAsString(searchResults);
        } catch (Exception e) {
            throw new RuntimeException("Error searching coins on Binance: " + e.getMessage(), e);
        }
    }

    @Override
    public String getTop50CoinsByMarketRank() {
        String url = BINANCE_API_BASE_URL + "/api/v3/ticker/24hr";
        RestTemplate template = new RestTemplate();

        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, entity, String.class);

            List<Map<String, Object>> allCoins = objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});

            // Filter only USDT trading pairs and sort by volume
            List<Map<String, Object>> usdtPairs = allCoins.stream()
                    .filter(coin -> ((String)coin.get("symbol")).endsWith("USDT"))
                    .sorted((a, b) -> {
                        double volumeA = Double.parseDouble(a.get("quoteVolume").toString());
                        double volumeB = Double.parseDouble(b.get("quoteVolume").toString());
                        return Double.compare(volumeB, volumeA); // Sort by volume descending
                    })
                    .limit(50)
                    .collect(Collectors.toList());

            List<Map<String, Object>> formattedCoins = new ArrayList<>();
            int rank = 1;

            for (Map<String, Object> coinData : usdtPairs) {
                String symbol = (String)coinData.get("symbol");
                String baseAsset = symbol.replace("USDT", "");

                Map<String, Object> coin = new HashMap<>();
                coin.put("id", baseAsset.toLowerCase());
                coin.put("symbol", baseAsset.toLowerCase());
                coin.put("name", baseAsset);
                coin.put("market_cap_rank", rank++);
                coin.put("current_price", Double.parseDouble(coinData.get("lastPrice").toString()));
                coin.put("price_change_percentage_24h", Double.parseDouble(coinData.get("priceChangePercent").toString()));

                formattedCoins.add(coin);
            }

            return objectMapper.writeValueAsString(formattedCoins);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching top coins from Binance: " + e.getMessage(), e);
        }
    }

    @Override
    public String getTreadingCoins() {
        // For trending coins, we'll use the coins with highest 24h price increase
        String url = BINANCE_API_BASE_URL + "/api/v3/ticker/24hr";
        RestTemplate template = new RestTemplate();

        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, entity, String.class);

            List<Map<String, Object>> allCoins = objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});

            // Filter only USDT trading pairs with significant volume and sort by price change
            List<Map<String, Object>> trendingCoins = allCoins.stream()
                    .filter(coin -> {
                        String symbol = (String)coin.get("symbol");
                        double volume = Double.parseDouble(coin.get("quoteVolume").toString());
                        return symbol.endsWith("USDT") && volume > 1000000; // Only include coins with significant volume
                    })
                    .sorted((a, b) -> {
                        double changeA = Double.parseDouble(a.get("priceChangePercent").toString());
                        double changeB = Double.parseDouble(b.get("priceChangePercent").toString());
                        return Double.compare(changeB, changeA); // Sort by price change descending
                    })
                    .limit(7) // Top 7 trending coins
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> coins = new ArrayList<>();

            for (Map<String, Object> coinData : trendingCoins) {
                String symbol = (String)coinData.get("symbol");
                String baseAsset = symbol.replace("USDT", "");

                Map<String, Object> item = new HashMap<>();
                Map<String, Object> coin = new HashMap<>();

                coin.put("id", baseAsset.toLowerCase());
                coin.put("name", baseAsset);
                coin.put("symbol", baseAsset.toLowerCase());
                coin.put("market_cap_rank", 0);

                item.put("item", coin);
                coins.add(item);
            }

            result.put("coins", coins);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching trending coins from Binance: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to convert Binance API data to our Coin model
     */
    // In your CoinServiceImple.java
    private Coin mapBinanceCoinData(Map<String, Object> coinData, int rank) {
        String symbol = (String)coinData.get("symbol");
        String baseAsset = symbol.replace("USDT", "");

        Coin coin = new Coin();

        // Set basic information
        coin.setId(baseAsset.toLowerCase());
        coin.setSymbol(baseAsset.toLowerCase());
        coin.setName(baseAsset);

        // Set price information
        coin.setCurrentPrice(Double.parseDouble(coinData.get("lastPrice").toString()));
        coin.setPriceChange24h(Double.parseDouble(coinData.get("priceChange").toString()));
        coin.setPriceChangePercentage24h(Double.parseDouble(coinData.get("priceChangePercent").toString()));

        // Set market information
        coin.setHigh24h(Double.parseDouble(coinData.get("highPrice").toString()));
        coin.setLow24h(Double.parseDouble(coinData.get("lowPrice").toString()));
        coin.setTotalVolume((long)Double.parseDouble(coinData.get("quoteVolume").toString()));

        // Set rank if provided
        if (rank > 0) {
            coin.setMarketCapRank(rank);
        }

        // Estimate market cap based on volume (not precise but provides a relative value)
        long estimatedMarketCap = (long)(Double.parseDouble(coinData.get("quoteVolume").toString()) * 10);
        coin.setMarketCap(estimatedMarketCap);

        // Set image URL - use a reliable source that works with symbol
        String symbol_lower = baseAsset.toLowerCase();
        coin.setImage("https://lcw.nyc3.cdn.digitaloceanspaces.com/production/currencies/64/" + symbol_lower + ".webp");

        // Set current date for last update
        coin.setLastUpdate(new Date());

        return coin;
    }

    /**
     * Helper method to get Binance symbol from coin ID
     */
    private String getSymbolFromCoinId(String coinId) {
        // Map of known coin IDs to Binance symbols
        Map<String, String> symbolMap = new HashMap<>();
        symbolMap.put("bitcoin", "BTCUSDT");
        symbolMap.put("ethereum", "ETHUSDT");
        symbolMap.put("ripple", "XRPUSDT");
        symbolMap.put("bitcoin-cash", "BCHUSDT");
        symbolMap.put("cardano", "ADAUSDT");
        symbolMap.put("litecoin", "LTCUSDT");
        symbolMap.put("binancecoin", "BNBUSDT");
        symbolMap.put("tether", "USDTBUSD"); // Tether is special case
        // Add more mappings as needed

        // Try to find a direct mapping
        String symbol = symbolMap.get(coinId.toLowerCase());

        // If we have a direct mapping, use it
        if (symbol != null) {
            return symbol;
        }

        // Otherwise try to extract the symbol from the ID and add USDT
        // Remove common prefixes/suffixes that might be in the ID
        String cleanId = coinId
                .toLowerCase()
                .replace("-coin", "")
                .replace("-token", "")
                .replace("-network", "")
                .replace("the-", "")
                .replace("-protocol", "");

        // Try to match with common ticker formats
        if (cleanId.length() <= 5) {  // Most crypto tickers are 2-5 characters
            return cleanId.toUpperCase() + "USDT";
        }

        // For longer names, try to use just the first part
        String[] parts = cleanId.split("-");
        if (parts.length > 0 && parts[0].length() <= 10) {
            return parts[0].toUpperCase() + "USDT";
        }

        // If all else fails, just use the original ID
        return coinId.toUpperCase() + "USDT";
    }
}