package com.tradeswift.controllers;
import com.tradeswift.domain.WalletTransactionType;
import com.tradeswift.dto.TransactionDTO;
import com.tradeswift.models.*;
import com.tradeswift.repositories.TransactionHistoryRepository;
import com.tradeswift.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private PaymentOrderService paymentOrderService;


    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private TransactionHistoryService transactionHistoryService;


    @GetMapping
    public ResponseEntity<Wallet> getUserWallet(@RequestHeader("Authorization")String jwt) throws Exception {
        User user  = this.userService.findUserProfileByJwt(jwt);

        Wallet wallet = this.walletService.getUserWallet(user);
        return new ResponseEntity<>(wallet, HttpStatus.ACCEPTED);
    }

    @PutMapping("/{walletId}/transfer")
    public ResponseEntity<Wallet>walletToWalletTransfer(@RequestHeader("Authorization")String jwt,
                                                        @PathVariable Long walletId,
                                                        @RequestBody WalletTransaction req) throws Exception {

        User senderuser  = this.userService.findUserProfileByJwt(jwt);
        Wallet receiverWallet = this.walletService.findWalletById(walletId);
        Wallet wallet = this.walletService.walletToWalletTransfer(senderuser,receiverWallet,req.getAmount());


        WalletTransaction walletTransaction = new WalletTransaction();
        walletTransaction.setWallet(wallet);
        walletTransaction.setAmount(req.getAmount());
        walletTransaction.setDate(LocalDate.now());
        walletTransaction.setWalletTransactionType(WalletTransactionType.WITHDRAWAL);
        walletTransaction.setTransferId(receiverWallet.getId());
        walletTransaction.setPurpose(req.getPurpose());
        this.transactionHistoryRepository.save(walletTransaction);

        return new ResponseEntity<>(wallet,HttpStatus.ACCEPTED);

    }
    @PutMapping("/order/{orderId}/pay")
    public ResponseEntity<Wallet>walletToWalletTransfer(@RequestHeader("Authorization")String jwt, @PathVariable Long orderId) throws Exception {

        User user  = this.userService.findUserProfileByJwt(jwt);
        Order order = this.orderService.getOrderById(orderId);
        Wallet wallet = this.walletService.payOrderPayment(order,user);



        return new ResponseEntity<>(wallet,HttpStatus.ACCEPTED);

    }
    @PutMapping("/deposit")
    public ResponseEntity<Wallet>addMoneyToWallet(@RequestHeader("Authorization")String jwt,
                                                  @RequestParam(name = "order_id")Long orderId,
                                                  @RequestParam(name = "payment_id") String paymentId) throws Exception {

        User user  = this.userService.findUserProfileByJwt(jwt);

        Wallet wallet = this.walletService.getUserWallet(user);

        PaymentOrder order =  this.paymentOrderService.getPaymentOrderById(orderId);
        
        // Check if transaction with this payment ID already exists
        boolean transactionExists = this.transactionHistoryRepository.existsByPaymentId(paymentId);
        
        if (transactionExists) {
            System.out.println("Payment ID " + paymentId + " already processed. Skipping duplicate transaction creation.");
            return new ResponseEntity<>(wallet, HttpStatus.ACCEPTED);
        }

        Boolean status = this.paymentOrderService.proccedPaymentOrder(order,paymentId);

        if(status){
            wallet = this.walletService.addBalance(wallet,order.getAmount());

            // Create transaction record for the deposit,
            WalletTransaction walletTransaction = new WalletTransaction();
            walletTransaction.setWallet(wallet);
            walletTransaction.setAmount(order.getAmount());
            walletTransaction.setDate(LocalDate.now());
            walletTransaction.setWalletTransactionType(WalletTransactionType.ADD_MONEY);
            walletTransaction.setPurpose("Deposit amount");

            // Save and log the transaction
            WalletTransaction savedTransaction = this.transactionHistoryRepository.save(walletTransaction);
            System.out.println("Created deposit transaction with ID: " + savedTransaction.getId() +
                    " for amount: " + order.getAmount() +
                    " for wallet ID: " + wallet.getId());

            // Emergency database check after deposit
            System.out.println("🔍 DEPOSIT - EMERGENCY DATABASE CHECK 🔍");
            java.util.List<WalletTransaction> allTransactions =
                    this.transactionHistoryRepository.findAllTransactions();
            System.out.println("Total transactions in database: " + allTransactions.size());

            // Count transactions for this wallet
            int walletTransactionCount = 0;
            for (WalletTransaction tx : allTransactions) {
                if (tx.getWallet() != null && tx.getWallet().getId().equals(wallet.getId())) {
                    walletTransactionCount++;
                    System.out.println("MATCH: " + tx);
                }
            }
            System.out.println("Found " + walletTransactionCount + " transactions for wallet ID " + wallet.getId());
        }

        return new ResponseEntity<>(wallet,HttpStatus.ACCEPTED);

    }
    @GetMapping("/transaction")
    public ResponseEntity<List<TransactionDTO>> getUserTransactionHistory(@RequestHeader("Authorization")String jwt) throws Exception {
        System.out.println("⭐ Received request for transaction history");

        // Get original transactions
        List<WalletTransaction> userTransactionHistory = this.transactionHistoryService.getUserTransactionHistory(jwt);
        System.out.println("⭐ Found " + userTransactionHistory.size() + " transactions in database");

        // Convert to DTOs to ensure proper serialization
        List<TransactionDTO> transactionDTOs = new java.util.ArrayList<>();
        for (WalletTransaction tx : userTransactionHistory) {
            transactionDTOs.add(new TransactionDTO(tx));
        }

        System.out.println("⭐ Created " + transactionDTOs.size() + " transaction DTOs");

        // Log first transaction if available
        if (!transactionDTOs.isEmpty()) {
            System.out.println("⭐ First DTO: " + transactionDTOs.get(0));
        }

        return new ResponseEntity<>(transactionDTOs, HttpStatus.ACCEPTED);
    }

}