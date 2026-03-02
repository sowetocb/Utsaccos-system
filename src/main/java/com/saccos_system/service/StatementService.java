package com.saccos_system.service;
import com.saccos_system.dto.StatementDTO.StatementRequestDTO;
import com.saccos_system.dto.StatementDTO.StatementResponseDTO;
import com.saccos_system.dto.StatementDTO.StatementSummaryDTO;
import com.saccos_system.dto.StatementDTO.StatementTransactionDTO;
import com.saccos_system.model.*;
        import com.saccos_system.repository.*;
import com.saccos_system.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
        import java.util.stream.Collectors;
@Data
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StatementService {

    private final JwtTokenUtil jwtTokenUtil;
    private final SystemUserRepository userRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final TransactionRecordRepository transactionRepository;
    private final MonthlyStatementRepository statementRepository;
    private final NotificationService notificationService;

    public StatementResponseDTO generateMonthlyStatement(String token, StatementRequestDTO request) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        // Check if statement already exists
        Optional<MonthlyStatement> existingStatement = statementRepository
                .findByProfile_ProfileIdAndMonthAndYear(
                        user.getProfile().getProfileId(), request.getMonth(), request.getYear());

        if (existingStatement.isPresent()) {
            return mapToStatementResponse(existingStatement.get());
        }

        SavingsAccount savingsAccount = getSavingsAccount(user);

        // Calculate period
        YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // Get opening balance (closing balance of previous month)
        BigDecimal openingBalance = calculateOpeningBalance(savingsAccount, startDate);

        //  FIXED: Changed from findBySavingIdAndTransactionDateBetweenOrderByTransactionDate
        // to findBySavingsAccount_SavingIdAndTransactionDateBetweenOrderByTransactionDate
        List<TransactionRecord> transactions = transactionRepository
                .findBySavingsAccount_SavingIdAndTransactionDateBetweenOrderByTransactionDate(
                        savingsAccount.getSavingId(), startDate, endDate);

        // Calculate totals
        BigDecimal totalDeposits = calculateTotal(transactions, "DEPOSIT");
        BigDecimal totalWithdrawals = calculateTotal(transactions, "WITHDRAWAL");
        BigDecimal totalInterest = calculateTotal(transactions, "INTEREST");

        BigDecimal closingBalance = openingBalance
                .add(totalDeposits)
                .subtract(totalWithdrawals)
                .add(totalInterest);

        // Generate statement number
        String statementNumber = "STMT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create and save statement
        MonthlyStatement statement = new MonthlyStatement();
        statement.setStatementNumber(statementNumber);
        statement.setProfile(user.getProfile());
        statement.setMonth(request.getMonth());
        statement.setYear(request.getYear());
        statement.setOpeningBalance(openingBalance);
        statement.setTotalDeposits(totalDeposits);
        statement.setTotalWithdrawals(totalWithdrawals);
        statement.setTotalInterest(totalInterest);
        statement.setClosingBalance(closingBalance);
        statement.setGeneratedBy(user.getUsername());

        MonthlyStatement savedStatement = statementRepository.save(statement);

        // Send notification
        notificationService.sendStatementGeneratedNotification(user, savedStatement);

        return mapToStatementResponse(savedStatement, transactions);
    }

    public List<StatementSummaryDTO> getStatementHistory(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        List<MonthlyStatement> statements = statementRepository
                .findByProfile_ProfileIdOrderByYearDescMonthDesc(user.getProfile().getProfileId());

        return statements.stream()
                .map(this::mapToStatementSummary)
                .collect(Collectors.toList());
    }

    public StatementResponseDTO getStatementByNumber(String token, String statementNumber) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        MonthlyStatement statement = statementRepository.findByStatementNumber(statementNumber)
                .orElseThrow(() -> new RuntimeException("Statement not found"));

        // Verify statement belongs to user
        if (!statement.getProfile().getProfileId().equals(user.getProfile().getProfileId())) {
            throw new RuntimeException("Unauthorized access to statement");
        }

        // Get transactions for the statement period
        YearMonth yearMonth = YearMonth.of(statement.getYear(), statement.getMonth());
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        SavingsAccount savingsAccount = getSavingsAccount(user);


        List<TransactionRecord> transactions = transactionRepository
                .findBySavingsAccount_SavingIdAndTransactionDateBetweenOrderByTransactionDate(
                        savingsAccount.getSavingId(), startDate, endDate);

        return mapToStatementResponse(statement, transactions);
    }

    public byte[] downloadStatementPDF(String token, String statementNumber) {
        StatementResponseDTO statement = getStatementByNumber(token, statementNumber);
        String pdfContent = generatePDFContent(statement);
        return pdfContent.getBytes();
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

    private BigDecimal calculateOpeningBalance(SavingsAccount account, LocalDateTime startDate) {

        Optional<TransactionRecord> lastTransaction = transactionRepository
                .findTopBySavingsAccount_SavingIdAndTransactionDateBeforeOrderByTransactionDateDesc(
                        account.getSavingId(), startDate);

        if (lastTransaction.isPresent()) {
            return lastTransaction.get().getBalanceAfter();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTotal(List<TransactionRecord> transactions, String type) {
        return transactions.stream()
                .filter(t -> type.equals(t.getTransactionType()))
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private StatementResponseDTO mapToStatementResponse(MonthlyStatement statement) {
        return mapToStatementResponse(statement, Collections.emptyList());
    }

    private StatementResponseDTO mapToStatementResponse(MonthlyStatement statement,
                                                        List<TransactionRecord> transactions) {
        StatementResponseDTO dto = new StatementResponseDTO();
        dto.setStatementNumber(statement.getStatementNumber());
        dto.setPeriod(getMonthName(statement.getMonth()) + " " + statement.getYear());
        dto.setMemberName(statement.getProfile().getFirstName() + " " + statement.getProfile().getLastName());
        dto.setMemberNumber(statement.getProfile().getMemberNumber());

        Optional<SavingsAccount> accountOpt = savingsAccountRepository
                .findByProfile_ProfileId(statement.getProfile().getProfileId());
        accountOpt.ifPresent(account -> dto.setAccountNumber(account.getAccountNumber()));

        dto.setOpeningBalance(statement.getOpeningBalance());
        dto.setTotalDeposits(statement.getTotalDeposits());
        dto.setTotalWithdrawals(statement.getTotalWithdrawals());
        dto.setTotalInterest(statement.getTotalInterest());
        dto.setClosingBalance(statement.getClosingBalance());
        dto.setGeneratedDate(statement.getGeneratedDate());

        List<StatementTransactionDTO> transactionDTOs = transactions.stream()
                .map(this::mapToStatementTransaction)
                .collect(Collectors.toList());
        dto.setTransactions(transactionDTOs);

        return dto;
    }

    private StatementSummaryDTO mapToStatementSummary(MonthlyStatement statement) {
        StatementSummaryDTO dto = new StatementSummaryDTO();
        dto.setStatementNumber(statement.getStatementNumber());
        dto.setPeriod(getMonthName(statement.getMonth()) + " " + statement.getYear());
        dto.setOpeningBalance(statement.getOpeningBalance());
        dto.setClosingBalance(statement.getClosingBalance());
        dto.setGeneratedDate(statement.getGeneratedDate());
        dto.setIsSent(statement.getIsSent());
        return dto;
    }

    private StatementTransactionDTO mapToStatementTransaction(TransactionRecord transaction) {
        StatementTransactionDTO dto = new StatementTransactionDTO();
        dto.setDate(transaction.getTransactionDate().toLocalDate().toString());
        dto.setDescription(transaction.getDescription());
        dto.setReference(transaction.getTransactionRef());

        if ("DEPOSIT".equals(transaction.getTransactionType()) ||
                "INTEREST".equals(transaction.getTransactionType())) {
            dto.setDeposit(transaction.getAmount());
            dto.setWithdrawal(BigDecimal.ZERO);
        } else {
            dto.setDeposit(BigDecimal.ZERO);
            dto.setWithdrawal(transaction.getAmount());
        }

        dto.setBalance(transaction.getBalanceAfter());
        return dto;
    }

    private String getMonthName(int month) {
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return monthNames[month - 1];
    }

    private String generatePDFContent(StatementResponseDTO statement) {
        StringBuilder pdf = new StringBuilder();
        pdf.append("=== SACCO MONTHLY STATEMENT ===\n\n");
        pdf.append("Statement Number: ").append(statement.getStatementNumber()).append("\n");
        pdf.append("Period: ").append(statement.getPeriod()).append("\n");
        pdf.append("Member: ").append(statement.getMemberName()).append("\n");
        pdf.append("Member Number: ").append(statement.getMemberNumber()).append("\n");
        pdf.append("Account Number: ").append(statement.getAccountNumber()).append("\n\n");

        pdf.append("=== SUMMARY ===\n");
        pdf.append(String.format("Opening Balance:  %,.2f\n", statement.getOpeningBalance()));
        pdf.append(String.format("Total Deposits:   %,.2f\n", statement.getTotalDeposits()));
        pdf.append(String.format("Total Withdrawals:%,.2f\n", statement.getTotalWithdrawals()));
        pdf.append(String.format("Total Interest:   %,.2f\n", statement.getTotalInterest()));
        pdf.append(String.format("Closing Balance:  %,.2f\n\n", statement.getClosingBalance()));

        pdf.append("=== TRANSACTIONS ===\n");
        pdf.append("Date       | Description                | Deposit    | Withdrawal | Balance\n");
        pdf.append("-----------|----------------------------|------------|------------|------------\n");

        for (StatementTransactionDTO tx : statement.getTransactions()) {
            pdf.append(String.format("%-10s | %-26s | %10s | %10s | %,.2f\n",
                    tx.getDate(),
                    tx.getDescription().length() > 26 ? tx.getDescription().substring(0, 23) + "..." : tx.getDescription(),
                    tx.getDeposit().compareTo(BigDecimal.ZERO) > 0 ? String.format("%,.2f", tx.getDeposit()) : "",
                    tx.getWithdrawal().compareTo(BigDecimal.ZERO) > 0 ? String.format("%,.2f", tx.getWithdrawal()) : "",
                    tx.getBalance()));
        }

        pdf.append("\n=== END OF STATEMENT ===\n");
        pdf.append("Generated on: ").append(statement.getGeneratedDate()).append("\n");

        return pdf.toString();
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new RuntimeException("Invalid authorization header");
    }
}