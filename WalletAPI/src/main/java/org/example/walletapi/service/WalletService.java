package org.example.walletapi.service;

import jakarta.annotation.PostConstruct;
import org.example.walletapi.controller.WalletOperationRequest;
import org.example.walletapi.exception.InsufficientFundsException;
import org.example.walletapi.exception.WalletNotFoundException;
import org.example.walletapi.model.Wallet;
import org.example.walletapi.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CacheManager cacheManager;

    private Cache cache;

    @PostConstruct
    public void init() {
        cache = cacheManager.getCache("walletCache");
        if (cache == null) {
            throw new RuntimeException("Cache 'walletCache' is not available!");
        }
    }

    public void processOperation(WalletOperationRequest request) {
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setWalletId(request.getWalletId());
                    newWallet.setBalance(0L);
                    return newWallet;
                });

        System.out.println("Saving wallet: " + wallet);

        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        switch (request.getOperationType()) {
            case "DEPOSIT":
                wallet.setBalance(wallet.getBalance() + request.getAmount());
                break;
            case "WITHDRAW":
                if (wallet.getBalance() < request.getAmount()) {
                    throw new InsufficientFundsException("Insufficient funds");
                }
                wallet.setBalance(wallet.getBalance() - request.getAmount());
                break;
            default:
                throw new IllegalArgumentException("Invalid operation type");
        }

        walletRepository.save(wallet);

        System.out.println("Wallet saved: " + wallet);

        if (cache != null) {
            cache.put(wallet.getWalletId(), wallet);
        }
    }

    @Async
    public CompletableFuture<Void> processOperationAsync(WalletOperationRequest request) {
        return CompletableFuture.runAsync(() -> processOperation(request));
    }

    public Wallet getWallet(UUID walletId) {
        Wallet wallet = null;
        if (cache != null) {
            wallet = cache.get(walletId, Wallet.class);
        }

        if (wallet == null) {
            wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
            if (cache != null) {
                cache.put(walletId, wallet);
            }
        }
        return wallet;
    }


    public void evictWalletCache(UUID walletId) {
        if (cache != null) {
            cache.evict(walletId);
        }
    }
}
