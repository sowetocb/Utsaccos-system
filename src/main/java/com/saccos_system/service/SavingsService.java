package com.saccos_system.service;

import com.saccos_system.dto.SavingsDTO.DepositRequestDTO;
import com.saccos_system.dto.SavingsDTO.SavingsSummaryDTO;
import com.saccos_system.dto.SavingsDTO.WithdrawalRequestDTO;
import com.saccos_system.dto.TransactionDTO.TransactionResponseDTO;
import com.saccos_system.model.*;
import com.saccos_system.repository.*;
import com.saccos_system.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SavingsService {

    private final JwtTokenUtil jwtTokenUtil;
    private final SystemUserRepository userRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final TransactionRecordRepository transactionRepository;
    //private final SavingsTransactionTypeRepository transactionTypeRepository;
    private final NotificationService notificationService;

    public TransactionResponseDTO deposit(String token, DepositRequestDTO request) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        SavingsAccount savingsAccount = getSavingsAccount(user);

        // Validate deposit amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be greater than zero");
        }

        // Check maximum daily deposit limit (e.g., 100,000)
        BigDecimal dailyLimit = BigDecimal.valueOf(100000);
        if (request.getAmount().compareTo(dailyLimit) > 0) {
            throw new RuntimeException("Deposit amount exceeds daily limit of " + dailyLimit);
        }

        // Process deposit
        BigDecimal balanceBefore = savingsAccount.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.getAmount());

        // Update savings account balance
        savingsAccount.setBalance(balanceAfter);
        savingsAccountRepository.save(savingsAccount);

        // Create transaction record
        TransactionRecord transaction = createTransaction(
                savingsAccount,
                "DEPOSIT",
                request.getAmount(),
                balanceBefore,
                balanceAfter,
                "Deposit via " + request.getPaymentMethod(),
                user.getUsername()
        );

        // Send notification
        notificationService.sendDepositNotification(user, request.getAmount(), balanceAfter);

        return mapToTransactionResponse(transaction);
    }

    public TransactionResponseDTO withdraw(String token, WithdrawalRequestDTO request) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        SavingsAccount savingsAccount = getSavingsAccount(user);

        // Validate withdrawal amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be greater than zero");
        }

        // Check minimum balance
        BigDecimal minimumBalance = BigDecimal.valueOf(1000); // Minimum balance requirement
        if (savingsAccount.getBalance().subtract(request.getAmount()).compareTo(minimumBalance) < 0) {
            throw new RuntimeException("Withdrawal would leave account below minimum balance of " + minimumBalance);
        }

        // Check daily withdrawal limit (e.g., 50,000)
        BigDecimal dailyLimit = BigDecimal.valueOf(50000);
        if (request.getAmount().compareTo(dailyLimit) > 0) {
            throw new RuntimeException("Withdrawal amount exceeds daily limit of " + dailyLimit);
        }

        // Check if account is active
        if (savingsAccount.getStatus() != null &&
                !"ACCOUNT_OPEN".equals(savingsAccount.getStatus().getStatusCode())) {
            throw new RuntimeException("Account is not active for withdrawals");
        }

        // Process withdrawal
        BigDecimal balanceBefore = savingsAccount.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(request.getAmount());

        // Update savings account balance
        savingsAccount.setBalance(balanceAfter);
        savingsAccountRepository.save(savingsAccount);

        // Create transaction record
        TransactionRecord transaction = createTransaction(
                savingsAccount,
                "WITHDRAWAL",
                request.getAmount(),
                balanceBefore,
                balanceAfter,
                "Withdrawal to " + request.getAccountNumber(),
                user.getUsername()
        );

        // Send notification
        notificationService.sendWithdrawalNotification(user, request.getAmount(), balanceAfter);

        return mapToTransactionResponse(transaction);
    }

    public List<TransactionResponseDTO> getTransactionHistory(String token, Integer limit) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        SavingsAccount savingsAccount = getSavingsAccount(user);

        List<TransactionRecord> transactions;
        if (limit != null && limit > 0) {
            //  Changed from findTopNBySavingIdOrderByTransactionDateDesc to findTopNBySavingsAccount_SavingIdOrderByTransactionDateDesc
            transactions = transactionRepository
                    .findTopNBySavingsAccount_SavingIdOrderByTransactionDateDesc(savingsAccount.getSavingId(), limit);
        } else {
            //  Changed from findBySavingIdOrderByTransactionDateDesc to findBySavingsAccount_SavingIdOrderByTransactionDateDesc
            transactions = transactionRepository
                    .findBySavingsAccount_SavingIdOrderByTransactionDateDesc(savingsAccount.getSavingId());
        }

        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    public SavingsSummaryDTO getSavingsSummary(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        SavingsAccount savingsAccount = getSavingsAccount(user);

        SavingsSummaryDTO summary = new SavingsSummaryDTO();
        summary.setAccountNumber(savingsAccount.getAccountNumber());
        summary.setCurrentBalance(savingsAccount.getBalance());
        summary.setInterestRate(savingsAccount.getInterestRate());
        summary.setAccountType(savingsAccount.getAccountType());
        summary.setAccountStatus(savingsAccount.getStatus().getStatusName());

        // Calculate monthly interest
        if (savingsAccount.getInterestRate() != null) {
            BigDecimal monthlyInterest = savingsAccount.getBalance()
                    .multiply(savingsAccount.getInterestRate())
                    .divide(BigDecimal.valueOf(1200), 2, BigDecimal.ROUND_HALF_UP);
            summary.setEstimatedMonthlyInterest(monthlyInterest);
        }

        // Get last month's transactions
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        List<TransactionRecord> recentTransactions = transactionRepository
                .findBySavingsAccount_SavingIdAndTransactionDateAfterOrderByTransactionDateDesc(
                        savingsAccount.getSavingId(), oneMonthAgo);

        summary.setRecentTransactions(recentTransactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList()));

        return summary;
    }

    // Helper methods
    private SystemUser getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private SavingsAccount getSavingsAccount(SystemUser user) {
        return savingsAccountRepository.findByProfile_ProfileId(user.getProfile().getProfileId())
                .orElseThrow(() -> new RuntimeException("Savings account not found"));
    }

    private TransactionRecord createTransaction(SavingsAccount account, String type,
                                                BigDecimal amount, BigDecimal balanceBefore,
                                                BigDecimal balanceAfter, String description,
                                                String performedBy) {
        String transactionRef = "TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        TransactionRecord transaction = new TransactionRecord();
        transaction.setTransactionRef(transactionRef);
        transaction.setSavingsAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setPerformedBy(performedBy);

        return transactionRepository.save(transaction);

    }

    private TransactionResponseDTO mapToTransactionResponse(TransactionRecord transaction) {
        TransactionResponseDTO dto = new TransactionResponseDTO();
        dto.setTransactionRef(transaction.getTransactionRef());
        dto.setType(transaction.getTransactionType());
        dto.setAmount(transaction.getAmount());
        dto.setBalanceBefore(transaction.getBalanceBefore());
        dto.setBalanceAfter(transaction.getBalanceAfter());
        dto.setDescription(transaction.getDescription());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setStatus("SUCCESS");
        return dto;
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new RuntimeException("Invalid authorization header");
    }
}