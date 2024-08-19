package org.example.walletapi.service;

import org.example.walletapi.controller.WalletOperationRequest;
import org.example.walletapi.exception.InsufficientFundsException;
import org.example.walletapi.model.Wallet;
import org.example.walletapi.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private WalletService walletService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(cacheManager.getCache("walletCache")).thenReturn(cache);
    }

    @Test
    public void testProcessOperationDeposit() {
        WalletOperationRequest request = new WalletOperationRequest();
        UUID walletId = UUID.randomUUID();
        request.setWalletId(walletId);
        request.setOperationType("DEPOSIT");
        request.setAmount(1000L);

        Wallet existingWallet = new Wallet();
        existingWallet.setWalletId(walletId);
        existingWallet.setBalance(500L);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.processOperation(request);

        assertEquals(1500L, existingWallet.getBalance());
        verify(walletRepository, times(1)).save(existingWallet);
    }

    @Test
    public void testProcessOperationWithdraw() {
        WalletOperationRequest request = new WalletOperationRequest();
        UUID walletId = UUID.randomUUID();
        request.setWalletId(walletId);
        request.setOperationType("WITHDRAW");
        request.setAmount(500L);

        Wallet existingWallet = new Wallet();
        existingWallet.setWalletId(walletId);
        existingWallet.setBalance(1000L);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.processOperation(request);

        assertEquals(500L, existingWallet.getBalance());
        verify(walletRepository, times(1)).save(existingWallet);
    }

    @Test
    public void testProcessWithdrawInsufficientFunds() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setWalletId(walletId);
        wallet.setBalance(500L);

        WalletOperationRequest request = new WalletOperationRequest();
        request.setWalletId(walletId);
        request.setOperationType("WITHDRAW");
        request.setAmount(1000L);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        InsufficientFundsException thrown = assertThrows(InsufficientFundsException.class, () -> {
            walletService.processOperation(request);
        });

        assertEquals("Insufficient funds", thrown.getMessage());
    }

    @Test
    public void testProcessOperationWalletNotFound() {
        WalletOperationRequest request = new WalletOperationRequest();
        UUID walletId = UUID.randomUUID();
        request.setWalletId(walletId);
        request.setOperationType("DEPOSIT");
        request.setAmount(1000L);

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.processOperation(request);

        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    public void testProcessOperationInvalidOperationType() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setWalletId(walletId);
        wallet.setBalance(500L);

        WalletOperationRequest request = new WalletOperationRequest();
        request.setWalletId(walletId);
        request.setOperationType("INVALID_OPERATION");
        request.setAmount(1000L);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            walletService.processOperation(request);
        });

        assertEquals("Invalid operation type", thrown.getMessage());
    }

    @Test
    public void testProcessOperationNegativeAmount() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setWalletId(walletId);
        wallet.setBalance(500L);

        WalletOperationRequest request = new WalletOperationRequest();
        request.setWalletId(walletId);
        request.setOperationType("DEPOSIT");
        request.setAmount(-1000L);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            walletService.processOperation(request);
        });

        assertEquals("Amount must be positive", thrown.getMessage());
    }

    @Test
    public void testProcessOperationWithdrawInsufficientFunds() {
        WalletOperationRequest request = new WalletOperationRequest();
        UUID walletId = UUID.randomUUID();
        request.setWalletId(walletId);
        request.setOperationType("WITHDRAW");
        request.setAmount(500L);

        Wallet existingWallet = new Wallet();
        existingWallet.setWalletId(walletId);
        existingWallet.setBalance(300L);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(existingWallet));

        InsufficientFundsException thrown = assertThrows(InsufficientFundsException.class, () -> {
            walletService.processOperation(request);
        });

        assertEquals("Insufficient funds", thrown.getMessage());
    }

    @Test
    public void testCacheableMethod() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setWalletId(walletId);
        wallet.setBalance(1000L);

        when(cache.get(walletId)).thenReturn(null);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWallet(walletId);

        assertNotNull(result, "Wallet should not be null");
        assertEquals(walletId, result.getWalletId(), "Wallet ID should match");
        assertEquals(1000L, result.getBalance(), "Wallet balance should match");

        verify(cache).put(walletId, wallet);
    }

    @Test
    public void testCacheEviction() {
        UUID walletId = UUID.randomUUID();
        walletService.evictWalletCache(walletId);

        verify(cache).evict(walletId);
    }
}