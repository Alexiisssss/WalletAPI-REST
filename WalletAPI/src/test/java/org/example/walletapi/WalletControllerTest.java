package org.example.walletapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.walletapi.controller.WalletController;
import org.example.walletapi.controller.WalletOperationRequest;
import org.example.walletapi.service.WalletService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(WalletController.class)
public class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @Autowired
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testHandleOperation_Success() throws Exception {
        WalletOperationRequest request = new WalletOperationRequest();
        request.setWalletId(UUID.randomUUID());
        request.setOperationType("WITHDRAW");
        request.setAmount(1000L);

        Mockito.when(walletService.processOperationAsync(Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        MvcResult result = mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("Operation successful", result.getResponse().getContentAsString());
    }

    @Test
    public void testHandleOperation_Failure() throws Exception {
        WalletOperationRequest request = new WalletOperationRequest();
        request.setWalletId(UUID.randomUUID());
        request.setOperationType("WITHDRAW");
        request.setAmount(1000L);

        Mockito.when(walletService.processOperationAsync(Mockito.any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test exception")));

        MvcResult result = mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andReturn();

        assertEquals("Operation failed: Test exception", result.getResponse().getContentAsString());
    }
}