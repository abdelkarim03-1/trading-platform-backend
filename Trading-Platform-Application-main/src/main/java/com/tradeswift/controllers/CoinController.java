package com.tradeswift.controllers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeswift.models.Coin;
import com.tradeswift.service.CoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("coins")
public class CoinController {

    @Autowired
    private CoinService coinService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Coin>> getCoinList(@RequestParam(name = "page", required = false, defaultValue = "1") int page) {
        List<Coin> coinList = this.coinService.getCoinList(page);
        return new ResponseEntity<>(coinList, HttpStatus.OK);
    }

    @GetMapping("/{coinId}")
    public ResponseEntity<Coin> findById(@PathVariable String coinId) {
        Coin coin = this.coinService.findById(coinId);
        return new ResponseEntity<>(coin, HttpStatus.OK);
    }

    @GetMapping("/{coinId}/chart")
    public ResponseEntity<JsonNode> getMarketChart(
            @PathVariable String coinId,
            @RequestParam(name = "days", defaultValue = "7") int days) {

        try {
            String marketChart = this.coinService.getMarketChart(coinId, days);
            System.out.println("Chart response for " + coinId + ": " + marketChart);

            // Ensure the response has the expected structure
            if (!marketChart.contains("\"prices\":")) {
                marketChart = "{\"prices\":[]}";
            }

            // Convert the string to a JsonNode so that the client gets a proper JSON object
            JsonNode jsonNode = objectMapper.readTree(marketChart);
            return new ResponseEntity<>(jsonNode, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error getting chart for " + coinId + ": " + e.getMessage());
            // Return an empty but valid data structure
            try {
                return new ResponseEntity<>(objectMapper.readTree("{\"prices\":[]}"), HttpStatus.OK);
            } catch (JsonProcessingException ex) {
                // Should never happen, but handle if necessary
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }


    @GetMapping("/search")
    ResponseEntity<JsonNode> searchCoin(@RequestParam(name = "q") String keywords) {
        String coin = this.coinService.searchCoin(keywords);
        JsonNode jsonNode = null;
        try {
            jsonNode = this.objectMapper.readTree(coin);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return new ResponseEntity<>(jsonNode, HttpStatus.OK);
    }

    @GetMapping("/top50")
    ResponseEntity<JsonNode> getTop50CoinByMarketChart() {
        String coin = this.coinService.getTop50CoinsByMarketRank();
        JsonNode jsonNode = null;
        try {
            jsonNode = this.objectMapper.readTree(coin);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return new ResponseEntity<>(jsonNode, HttpStatus.OK);
    }

    @GetMapping("/trending")
    ResponseEntity<JsonNode> getTreadingCoin() throws JsonProcessingException {
        String coin = this.coinService.getTreadingCoins();
        JsonNode jsonNode = null;
        try {
            jsonNode = this.objectMapper.readTree(coin);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(jsonNode);
    }

    @GetMapping("/details/{coinId}")
    ResponseEntity<JsonNode> getCoinDetails(@PathVariable String coinId) throws JsonProcessingException {
        String coin = coinService.getCoinDetails(coinId);
        JsonNode jsonNode = null;
        try {
            jsonNode = this.objectMapper.readTree(coin);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(jsonNode);
    }
}