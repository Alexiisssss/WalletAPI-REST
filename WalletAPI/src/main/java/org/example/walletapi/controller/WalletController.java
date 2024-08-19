package org.example.walletapi.controller;

import org.example.walletapi.model.Wallet;
import org.example.walletapi.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private static final Logger logger = Logger.getLogger(WalletController.class.getName());
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<String>> handleOperation(@Valid @RequestBody WalletOperationRequest request) {
        return walletService.processOperationAsync(request)
                .thenApply(aVoid -> ResponseEntity.ok("Operation successful"))
                .exceptionally(ex -> {
                    logger.severe("Operation failed: " + ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Operation failed: " + ex.getMessage());
                });
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<Wallet> getBalance(@PathVariable UUID walletId) {
        Wallet wallet = walletService.getWallet(walletId);
        if (wallet == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(wallet);
    }
}
