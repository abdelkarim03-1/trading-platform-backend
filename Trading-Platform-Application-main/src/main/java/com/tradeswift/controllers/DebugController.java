package com.tradeswift.controllers;

import com.tradeswift.utils.DatabaseDebugger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for debugging operations
 * Note: This should be disabled or secured in production
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private DatabaseDebugger databaseDebugger;

    /**
     * Run database diagnostics for wallet transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> debugTransactions() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Run diagnostics
            databaseDebugger.debugTransactionWalletRelationships();
            response.put("status", "success");
            response.put("message", "Diagnostics completed, check server logs");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error running diagnostics: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Fix database issues with wallet transactions
     */
    @GetMapping("/fix-transactions")
    public ResponseEntity<Map<String, Object>> fixTransactions() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Fix issues
            databaseDebugger.fixMissingWalletReferences();
            response.put("status", "success");
            response.put("message", "Fixes attempted, check server logs");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error fixing issues: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}