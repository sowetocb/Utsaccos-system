package com.saccos_system.service;

import com.saccos_system.dto.*;
import com.saccos_system.model.*;
import com.saccos_system.model.LoanType;
import com.saccos_system.repository.*;
import com.saccos_system.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final JwtTokenUtil jwtTokenUtil;
    private final SystemUserRepository systemUserRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final LoanPaymentScheduleRepository loanPaymentScheduleRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final TransactionRecordRepository transactionRepository;
    private final LookupStatusRepository lookupStatusRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final SavingsTransactionTypeRepository savingsTransactionTypeRepository;
    private final NotificationRepository notificationRepository;
    private final MonthlyStatementRepository monthlyStatementRepository;
    private final RoleService roleService;
    private final SMSService smsService;

    // ========== ADMIN VALIDATION ==========

    private SystemUser validateAdmin(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = systemUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user has ADMIN role
        if (!roleService.userHasRole(userId, "ADMIN")) {
            log.warn("Unauthorized admin access attempt by user: {} (ID: {})",
                    user.getUsername(), userId);
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        return user;
    }

    // Validate Loan Officer role
    private SystemUser validateLoanOfficer(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = systemUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!roleService.userHasRole(userId, "LOAN_OFFICER") &&
                !roleService.userHasRole(userId, "ADMIN")) {
            throw new RuntimeException("Unauthorized: Loan officer access required");
        }

        return user;
    }

    // Validate Accountant role
    private SystemUser validateAccountant(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = systemUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!roleService.userHasRole(userId, "ACCOUNTANT") &&
                !roleService.userHasRole(userId, "ADMIN")) {
            throw new RuntimeException("Unauthorized: Accountant access required");
        }

        return user;
    }

    private String getAdminUsername(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        return systemUserRepository.findById(userId)
                .map(SystemUser::getUsername)
                .orElse("unknown");
    }

    // ========== USER MANAGEMENT ==========

    public List<AdminUserDTO> getAllUsers(String token) {
        SystemUser admin = validateAdmin(token);
        log.info("Admin {} requested all users list", admin.getUsername());

        List<SystemUser> users = systemUserRepository.findAll();

        return users.stream()
                .map(this::mapToAdminUserDTO)
                .collect(Collectors.toList());
    }

    public void updateUserStatus(String token, Long userId, Boolean isActive) {
        SystemUser admin = validateAdmin(token);

        SystemUser user = systemUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(isActive);
        user.setModifiedBy(admin.getUsername());
        systemUserRepository.save(user);

        log.info("Admin {} updated user {} status to {}",
                admin.getUsername(), userId, isActive);
    }

    public void unlockUserAccount(String token, Long userId) {
        SystemUser admin = validateAdmin(token);

        SystemUser user = systemUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsLocked(false);
        user.setFailedLoginAttempts(0);
        user.setModifiedBy(admin.getUsername());
        systemUserRepository.save(user);

        log.info("Admin {} unlocked user {}", admin.getUsername(), userId);
    }

    // Assign role to user
    public void assignRole(String token, Long userId, String roleName) {
        SystemUser admin = validateAdmin(token);

        roleService.assignRoleToUser(userId, roleName, admin.getUsername());

        log.info("Admin {} assigned role {} to user {}",
                admin.getUsername(), roleName, userId);
    }

    // Remove role from user
    public void removeRole(String token, Long userId, String roleName) {
        SystemUser admin = validateAdmin(token);

        // Prevent removing ADMIN role from yourself
        if (roleName.equals("ADMIN") && admin.getUserId().equals(userId)) {
            throw new RuntimeException("Cannot remove ADMIN role from yourself");
        }

        roleService.removeRoleFromUser(userId, roleName, admin.getUsername());

        log.info("Admin {} removed role {} from user {}",
                admin.getUsername(), roleName, userId);
    }

    // Get user roles
    public List<String> getUserRoles(String token, Long userId) {
        validateAdmin(token);
        return roleService.getUserRoles(userId);
    }

    public List<ActivityLogDTO> getUserActivity(String token, Long userId) {
        validateAdmin(token);

        List<AuthAuditLog> auditLogs = authAuditLogRepository.findByUserIdOrderByCreatedDateDesc(userId);

        return auditLogs.stream()
                .map(this::mapToActivityLogDTO)
                .collect(Collectors.toList());
    }

    // ========== LOAN MANAGEMENT ==========

    /**
     * Get all pending loan applications (regular and emergency)
     */
    public List<AdminLoanApplicationDTO> getPendingLoanApplications(String token) {
        validateLoanOfficer(token);

        List<LoanApplication> pendingApplications = loanApplicationRepository
                .findByStatus_StatusCode("LOAN_PENDING");

        return pendingApplications.stream()
                .map(this::mapToAdminLoanApplicationDTO)
                .collect(Collectors.toList());
    }


     // Get emergency pending applications (priority queue)
    public List<AdminLoanApplicationDTO> getEmergencyPendingApplications(String token) {
        validateLoanOfficer(token);

        List<LoanApplication> emergencyPending = loanApplicationRepository
                .findByStatus_StatusCodeOrderByAppliedDateAsc("LOAN_PENDING_EMERGENCY");

        return emergencyPending.stream()
                .map(this::mapToAdminLoanApplicationDTO)
                .collect(Collectors.toList());
    }


     // Expedite emergency loan review
    @Transactional
    public void expediteEmergencyLoan(String token, Long applicationId, EmergencyLoanDecisionDTO decision) {
        SystemUser officer = validateLoanOfficer(token);

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Loan application not found"));

        // Verify it's an emergency loan
        if (!"EMERGENCY".equals(application.getLoanType())) {
            throw new RuntimeException("Expedite workflow only for emergency loans");
        }

        // Log the decision for audit
        log.info("=== EMERGENCY LOAN DECISION ===");
        log.info("Application ID: {}", applicationId);
        log.info("Application Number: {}", application.getApplicationNumber());
        log.info("Officer: {}", officer.getUsername());
        log.info("Decision: {}", decision.isApproved() ? "APPROVED" : "REJECTED");
        log.info("Reason: {}", decision.getReason());
        log.info("Document Verified: {}", decision.isDocumentVerified());
        log.info("Phone Verified: {}", decision.isPhoneVerified());

        if (decision.isApproved()) {
            // Create approval DTO
            LoanApprovalDTO approval = new LoanApprovalDTO();
            approval.setApprovedAmount(decision.getApprovedAmount() != null ?
                    decision.getApprovedAmount() : application.getAmount());
            approval.setInterestRate(BigDecimal.valueOf(18.0)); // Higher interest for emergency loans
            approval.setApprovedBy(officer.getUsername());
            approval.setNotes(decision.getReason());

            // Approve with emergency terms
            approveEmergencyLoan(application, approval, officer, decision);
        } else {
            // Reject with reason
            LoanRejectionDTO rejection = new LoanRejectionDTO();
            rejection.setRejectionReason(decision.getReason());
            rejection.setRejectedBy(officer.getUsername());

            rejectLoanApplication(token, applicationId, rejection);
        }
    }

    /**
     * Approve emergency loan with immediate disbursement
     */
    @Transactional
    public void approveEmergencyLoan(LoanApplication application, LoanApprovalDTO approval,
                                     SystemUser officer, EmergencyLoanDecisionDTO decision) {

        // Update application status
        LookupStatus approvedStatus = lookupStatusRepository.findByStatusCode("LOAN_APPROVED")
                .orElseThrow(() -> new RuntimeException("Approved status not found"));

        application.setStatus(approvedStatus);
        application.setApprovedDate(LocalDateTime.now());
        application.setApprovedBy(officer.getUsername());
        application.setApprovedAmount(approval.getApprovedAmount());

        // Store verification details
        application.setDocumentVerified(decision.isDocumentVerified());
        application.setPhoneVerified(decision.isPhoneVerified());
        application.setVerificationNotes(decision.getVerificationNotes());

        loanApplicationRepository.save(application);

        // Create loan with emergency terms (higher interest)
        createEmergencyLoan(application, approval, officer);

        // Disburse funds immediately to savings account
        disburseEmergencyLoanFunds(application, officer);

        log.info("=== EMERGENCY LOAN APPROVED AND DISBURSED ===");
        log.info("Application: {}", application.getApplicationNumber());
        log.info("Amount: {}", approval.getApprovedAmount());
        log.info("Interest Rate: 18.0%");
        log.info("Document Verified: {}", decision.isDocumentVerified());
        log.info("Phone Verified: {}", decision.isPhoneVerified());
        log.info("Officer: {}", officer.getUsername());
        log.info("Disbursed to Member: {}", application.getProfile().getMemberNumber());
    }

    /**
     * Create emergency loan record
     */
    private void createEmergencyLoan(LoanApplication application, LoanApprovalDTO approval, SystemUser officer) {
        // Generate loan number with EMERG prefix
        String loanNumber = "EMERG-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%03d", application.getApplicationId());

        // Higher interest rate for emergency loans (18%)
        BigDecimal interestRate = BigDecimal.valueOf(18.0);

        // Calculate monthly payment (shorter term = higher payments)
        BigDecimal monthlyPayment = calculateMonthlyPayment(
                approval.getApprovedAmount(),
                interestRate,
                application.getTermMonths()
        );

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(application.getTermMonths());

        // Get active loan status
        LookupStatus activeStatus = lookupStatusRepository.findByStatusCode("LOAN_ACTIVE")
                .orElseThrow(() -> new RuntimeException("Active loan status not found"));

        // Create loan
        Loan loan = new Loan();
        loan.setLoanNumber(loanNumber);
        loan.setProfile(application.getProfile());
        loan.setPrincipalAmount(approval.getApprovedAmount());
        loan.setInterestRate(interestRate);
        loan.setLoanType(application.getLoanType());
        loan.setTermMonths(application.getTermMonths());
        loan.setStartDate(startDate);
        loan.setEndDate(endDate);
        loan.setMonthlyPayment(monthlyPayment);
        loan.setRemainingBalance(approval.getApprovedAmount());
        loan.setStatus(activeStatus);
        loan.setCreatedBy(officer.getUsername());

        loanRepository.save(loan);

        // Generate payment schedule
        generatePaymentSchedule(loan, approval.getApprovedAmount(), interestRate, application.getTermMonths());

        log.info("Emergency loan created: {} for amount: {}", loanNumber, approval.getApprovedAmount());
    }

    /**
     * Disburse emergency loan funds immediately to savings account
     */
    private void disburseEmergencyLoanFunds(LoanApplication application, SystemUser officer) {
        SavingsAccount savingsAccount = savingsAccountRepository
                .findByProfile_ProfileId(application.getProfile().getProfileId())
                .orElseThrow(() -> new RuntimeException("Savings account not found"));

        BigDecimal balanceBefore = savingsAccount.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(application.getApprovedAmount());

        // Update savings account balance
        savingsAccount.setBalance(balanceAfter);
        savingsAccountRepository.save(savingsAccount);

        // Record transaction
        TransactionRecord transaction = new TransactionRecord();
        transaction.setTransactionRef("LOAN-" + application.getApplicationNumber());
        transaction.setSavingsAccount(savingsAccount);
        transaction.setTransactionType("LOAN_DISBURSEMENT");
        transaction.setAmount(application.getApprovedAmount());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription("Emergency loan disbursement - " + application.getApplicationNumber());
        transaction.setPerformedBy(officer.getUsername());

        transactionRepository.save(transaction);

        log.info("Funds disbursed to account {}: Balance before: {}, after: {}",
                savingsAccount.getAccountNumber(), balanceBefore, balanceAfter);

        // Send SMS confirmation
        if (application.getProfile().getPhone() != null) {
            String message = String.format(
                    "URGENT: Your emergency loan of %,.2f has been approved and disbursed to your savings account. New balance: %,.2f",
                    application.getApprovedAmount(), balanceAfter);
            //smsService.sendTransactionAlert(application.getProfile().getPhone(), message);
            log.info("SMS notification sent to: {}", application.getProfile().getPhone());
        }
    }

    /**
     * Regular loan approval (existing method)
     */
    public void approveLoanApplication(String token, Long applicationId, LoanApprovalDTO approval) {
        SystemUser officer = validateLoanOfficer(token);

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Loan application not found"));

        // Check if it's an emergency loan - should use expedite workflow
        if ("EMERGENCY".equals(application.getLoanType())) {
            throw new RuntimeException("Emergency loans must use the expedite workflow");
        }

        // Update application status
        LookupStatus approvedStatus = lookupStatusRepository.findByStatusCode("LOAN_APPROVED")
                .orElseThrow(() -> new RuntimeException("Approved status not found"));

        application.setStatus(approvedStatus);
        application.setApprovedDate(LocalDateTime.now());
        application.setApprovedBy(officer.getUsername());
        application.setApprovedAmount(approval.getApprovedAmount());

        loanApplicationRepository.save(application);

        // Create loan record
        createLoanFromApplication(application, approval, officer);

        log.info("Loan officer {} approved regular loan application {}", officer.getUsername(), applicationId);
    }

    public void rejectLoanApplication(String token, Long applicationId, LoanRejectionDTO rejection) {
        SystemUser officer = validateLoanOfficer(token);

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Loan application not found"));

        LookupStatus rejectedStatus = lookupStatusRepository.findByStatusCode("LOAN_REJECTED")
                .orElseThrow(() -> new RuntimeException("Rejected status not found"));

        application.setStatus(rejectedStatus);
        application.setRejectionReason(rejection.getRejectionReason());

        loanApplicationRepository.save(application);

        log.info("Loan officer {} rejected loan application {} - Reason: {}",
                officer.getUsername(), applicationId, rejection.getRejectionReason());
    }

    // ========== REPORTS ==========

    public SavingsReportDTO getSavingsReport(String token, String startDateStr, String endDateStr) {
        validateAccountant(token);

        LocalDateTime startDate = parseStartDate(startDateStr);
        LocalDateTime endDate = parseEndDate(endDateStr);

        // Get all savings accounts
        List<SavingsAccount> allAccounts = savingsAccountRepository.findAll();

        // Calculate total balance
        BigDecimal totalBalance = allAccounts.stream()
                .map(SavingsAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get transactions for the period
        List<TransactionRecord> transactions = transactionRepository
                .findByTransactionDateBetween(startDate, endDate);

        BigDecimal monthlyDeposits = transactions.stream()
                .filter(t -> "DEPOSIT".equals(t.getTransactionType()))
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyWithdrawals = transactions.stream()
                .filter(t -> "WITHDRAWAL".equals(t.getTransactionType()))
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get top 10 accounts by balance
        List<AccountSummaryDTO> topAccounts = allAccounts.stream()
                .sorted((a1, a2) -> a2.getBalance().compareTo(a1.getBalance()))
                .limit(10)
                .map(this::mapToAccountSummaryDTO)
                .collect(Collectors.toList());

        SavingsReportDTO report = new SavingsReportDTO();
        report.setTotalAccounts(allAccounts.size());
        report.setTotalBalance(totalBalance);
        report.setMonthlyDeposits(monthlyDeposits);
        report.setMonthlyWithdrawals(monthlyWithdrawals);
        report.setTopAccounts(topAccounts);

        log.info("Savings report generated: {} accounts, total balance: {}",
                allAccounts.size(), totalBalance);
        return report;
    }

    public LoansReportDTO getLoansReport(String token, String startDateStr, String endDateStr) {
        validateAccountant(token);

        LocalDateTime startDate = parseStartDate(startDateStr);
        LocalDateTime endDate = parseEndDate(endDateStr);

        List<Loan> allLoans = loanRepository.findAll();
        List<Loan> activeLoans = loanRepository.findByStatus_StatusCode("LOAN_ACTIVE");

        // Separate emergency loans for reporting
        List<Loan> emergencyLoans = allLoans.stream()
                .filter(l -> "EMERGENCY".equals(l.getLoanType()))
                .collect(Collectors.toList());

        BigDecimal totalDisbursed = allLoans.stream()
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRepaid = allLoans.stream()
                .map(l -> l.getPrincipalAmount().subtract(l.getRemainingBalance()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingBalance = allLoans.stream()
                .map(Loan::getRemainingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LoanSummaryDTO> activeLoanDetails = activeLoans.stream()
                .map(this::mapToLoanSummaryDTO)
                .collect(Collectors.toList());

        LoansReportDTO report = new LoansReportDTO();
        report.setTotalLoans(allLoans.size());
        report.setActiveLoans(activeLoans.size());
        report.setEmergencyLoans(emergencyLoans.size());
        report.setTotalDisbursed(totalDisbursed);
        report.setTotalRepaid(totalRepaid);
        report.setOutstandingBalance(outstandingBalance);
        report.setActiveLoanDetails(activeLoanDetails);

        log.info("Loans report generated: {} total loans, {} emergency loans",
                allLoans.size(), emergencyLoans.size());
        return report;
    }

    public TransactionsReportDTO getTransactionsReport(String token, String startDateStr, String endDateStr) {
        validateAccountant(token);

        LocalDateTime startDate = parseStartDate(startDateStr);
        LocalDateTime endDate = parseEndDate(endDateStr);

        List<TransactionRecord> transactions = transactionRepository
                .findByTransactionDateBetween(startDate, endDate);

        BigDecimal totalDeposits = transactions.stream()
                .filter(t -> "DEPOSIT".equals(t.getTransactionType()))
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawals = transactions.stream()
                .filter(t -> "WITHDRAWAL".equals(t.getTransactionType()))
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransactionSummaryDTO> recentTransactions = transactions.stream()
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .limit(20)
                .map(this::mapToTransactionSummaryDTO)
                .collect(Collectors.toList());

        TransactionsReportDTO report = new TransactionsReportDTO();
        report.setTotalTransactions(transactions.size());
        report.setTotalDeposits(totalDeposits);
        report.setTotalWithdrawals(totalWithdrawals);
        report.setRecentTransactions(recentTransactions);

        log.info("Transactions report generated: {} transactions", transactions.size());
        return report;
    }

    // ========== SYSTEM SETTINGS ==========

    public SystemSettingsDTO getSystemSettings(String token) {
        validateAdmin(token);

        SystemSettingsDTO settings = new SystemSettingsDTO();
        settings.setMinimumSavingsBalance(BigDecimal.valueOf(1000));
        settings.setMaximumDailyWithdrawal(BigDecimal.valueOf(50000));
        settings.setMaximumDailyDeposit(BigDecimal.valueOf(100000));
        settings.setLoanInterestRate(BigDecimal.valueOf(12.0));
        settings.setEmergencyLoanInterestRate(BigDecimal.valueOf(18.0));
        settings.setLoanTermMonths(12);
        settings.setEmergencyLoanMaxTerm(6);
        settings.setMaintenanceMode(false);
        settings.setSystemMessage("System is operational");

        log.info("System settings retrieved by admin");
        return settings;
    }

    public void updateSystemSettings(String token, SystemSettingsDTO settings) {
        validateAdmin(token);
        log.info("System settings updated by admin: minimumSavings={}, maxWithdrawal={}, maxDeposit={}",
                settings.getMinimumSavingsBalance(),
                settings.getMaximumDailyWithdrawal(),
                settings.getMaximumDailyDeposit());
    }

    // ========== ADMIN DASHBOARD ==========

    public AdminDashboardDTO getAdminDashboard(String token) {
        validateAdmin(token);

        AdminDashboardDTO dashboard = new AdminDashboardDTO();

        // Member statistics
        List<StaffProfile> allMembers = staffProfileRepository.findAll();
        List<StaffProfile> activeMembers = allMembers.stream()
                .filter(m -> m.getStatus() != null && "MEMBER_ACTIVE".equals(m.getStatus().getStatusCode()))
                .collect(Collectors.toList());

        dashboard.setTotalMembers(allMembers.size());
        dashboard.setActiveMembers(activeMembers.size());

        // Financial statistics
        List<SavingsAccount> allSavingsAccounts = savingsAccountRepository.findAll();
        BigDecimal totalSavings = allSavingsAccounts.stream()
                .map(SavingsAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTotalSavings(totalSavings);

        List<Loan> allLoans = loanRepository.findAll();
        BigDecimal totalLoans = allLoans.stream()
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTotalLoans(totalLoans);

        // Pending applications (separate regular and emergency)
        List<LoanApplication> pendingRegular = loanApplicationRepository
                .findByStatus_StatusCode("LOAN_PENDING");
        List<LoanApplication> pendingEmergency = loanApplicationRepository
                .findByStatus_StatusCode("LOAN_PENDING_EMERGENCY");

        dashboard.setPendingApplications(pendingRegular.size());
        dashboard.setPendingEmergencyApplications(pendingEmergency.size());

        // Overdue loans
        List<Loan> overdueLoans = loanRepository.findOverdueLoans("LOAN_ACTIVE");
        dashboard.setOverdueLoans(overdueLoans.size());

        // Role counts
        dashboard.setTotalAdmins(roleService.getUsersByRole("ADMIN").size());
        dashboard.setTotalLoanOfficers(roleService.getUsersByRole("LOAN_OFFICER").size());
        dashboard.setTotalAccountants(roleService.getUsersByRole("ACCOUNTANT").size());

        // Trends
        dashboard.setSavingsTrend(generateTrendData("Savings"));
        dashboard.setLoansTrend(generateTrendData("Loans"));

        log.info("Admin dashboard generated: {} members, {} savings, {} loans, {} pending emergency",
                allMembers.size(), totalSavings, totalLoans, pendingEmergency.size());
        return dashboard;
    }

    // ========== HELPER METHODS ==========

    private LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now().minusMonths(1);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(dateStr, formatter).atStartOfDay();
    }

    private LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(dateStr, formatter).atTime(23, 59, 59);
    }

    private void createLoanFromApplication(LoanApplication application, LoanApprovalDTO approval, SystemUser officer) {
        // Generate loan number
        String loanNumber = "LN-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%03d", application.getApplicationId());

        // Calculate loan details
        BigDecimal interestRate = approval.getInterestRate() != null ?
                approval.getInterestRate() : BigDecimal.valueOf(12.0);
        BigDecimal monthlyPayment = calculateMonthlyPayment(
                approval.getApprovedAmount(),
                interestRate,
                application.getTermMonths()
        );

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(application.getTermMonths());

        // Get active loan status
        LookupStatus activeStatus = lookupStatusRepository.findByStatusCode("LOAN_ACTIVE")
                .orElseThrow(() -> new RuntimeException("Active loan status not found"));

        // Create loan
        Loan loan = new Loan();
        loan.setLoanNumber(loanNumber);
        loan.setProfile(application.getProfile());
        loan.setPrincipalAmount(approval.getApprovedAmount());
        loan.setInterestRate(interestRate);
        loan.setLoanType(application.getLoanType());
        loan.setTermMonths(application.getTermMonths());
        loan.setStartDate(startDate);
        loan.setEndDate(endDate);
        loan.setMonthlyPayment(monthlyPayment);
        loan.setRemainingBalance(approval.getApprovedAmount());
        loan.setStatus(activeStatus);
        loan.setCreatedBy(officer.getUsername());

        loanRepository.save(loan);

        // Generate payment schedule
        generatePaymentSchedule(loan, approval.getApprovedAmount(), interestRate, application.getTermMonths());
    }

    private void generatePaymentSchedule(Loan loan, BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = loan.getMonthlyPayment();

        BigDecimal remainingBalance = principal;

        for (int i = 1; i <= months; i++) {
            BigDecimal interestAmount = remainingBalance.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalAmount = monthlyPayment.subtract(interestAmount);

            if (i == months) {
                // Last payment - adjust for rounding
                principalAmount = remainingBalance;
                interestAmount = monthlyPayment.subtract(principalAmount);
            }

            LoanPaymentSchedule schedule = new LoanPaymentSchedule();
            schedule.setLoan(loan);
            schedule.setInstallmentNumber(i);
            schedule.setDueDate(loan.getStartDate().plusMonths(i));
            schedule.setAmountDue(monthlyPayment);
            schedule.setPrincipalAmount(principalAmount);
            schedule.setInterestAmount(interestAmount);
            schedule.setStatus("PENDING");

            loanPaymentScheduleRepository.save(schedule);

            remainingBalance = remainingBalance.subtract(principalAmount);
        }
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal ratePower = onePlusRate.pow(months);

        return principal
                .multiply(monthlyRate)
                .multiply(ratePower)
                .divide(ratePower.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    // ========== MAPPING METHODS ==========

    private AdminUserDTO mapToAdminUserDTO(SystemUser user) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());

        if (user.getProfile() != null) {
            dto.setFullName(user.getProfile().getFirstName() + " " + user.getProfile().getLastName());
            dto.setMemberNumber(user.getProfile().getMemberNumber());
        }

        dto.setIsActive(user.getIsActive());
        dto.setIsLocked(user.getIsLocked());
        dto.setLastLoginDate(user.getLastLoginDate());
        dto.setCreatedDate(user.getCreatedDate());
        dto.setRoles(roleService.getUserRoles(user.getUserId()));

        return dto;
    }

    private ActivityLogDTO mapToActivityLogDTO(AuthAuditLog log) {
        ActivityLogDTO dto = new ActivityLogDTO();
        dto.setTimestamp(log.getCreatedDate());
        dto.setAction(log.getAction());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        dto.setStatus(log.getStatus());
        return dto;
    }

    private AdminLoanApplicationDTO mapToAdminLoanApplicationDTO(LoanApplication app) {
        AdminLoanApplicationDTO dto = new AdminLoanApplicationDTO();
        dto.setApplicationId(app.getApplicationId());
        dto.setApplicationNumber(app.getApplicationNumber());

        if (app.getProfile() != null) {
            dto.setMemberName(app.getProfile().getFirstName() + " " + app.getProfile().getLastName());
            dto.setMemberNumber(app.getProfile().getMemberNumber());
        }

        dto.setLoanType(app.getLoanType());
        dto.setAmount(app.getAmount());
        dto.setTermMonths(app.getTermMonths());
        dto.setPurpose(app.getPurpose());
        dto.setAppliedDate(app.getAppliedDate());

        if (app.getStatus() != null) {
            dto.setStatus(app.getStatus().getStatusName());
        }

        // Map emergency fields with null safety
        dto.setEmergencyReason(app.getEmergencyReason());
        dto.setSupportingDocument(app.getSupportingDocument());
        dto.setDocumentVerified(app.getDocumentVerified() != null ? app.getDocumentVerified() : false);
        dto.setPhoneVerified(app.getPhoneVerified() != null ? app.getPhoneVerified() : false);
        dto.setVerificationNotes(app.getVerificationNotes());

        // Map guarantor fields
        dto.setGuarantorIdNumber(app.getGuarantorIdNumber());
        dto.setGuarantorName(app.getGuarantorName());
        dto.setGuarantorPhone(app.getGuarantorPhone());

        return dto;
    }

    private AccountSummaryDTO mapToAccountSummaryDTO(SavingsAccount account) {
        AccountSummaryDTO dto = new AccountSummaryDTO();
        if (account.getProfile() != null) {
            dto.setMemberNumber(account.getProfile().getMemberNumber());
            dto.setMemberName(account.getProfile().getFirstName() + " " + account.getProfile().getLastName());
        }
        dto.setAccountNumber(account.getAccountNumber());
        dto.setBalance(account.getBalance());
        return dto;
    }

    private LoanSummaryDTO mapToLoanSummaryDTO(Loan loan) {
        LoanSummaryDTO dto = new LoanSummaryDTO();
        dto.setLoanNumber(loan.getLoanNumber());
        dto.setLoanType(loan.getLoanType());
        dto.setPrincipalAmount(loan.getPrincipalAmount());
        dto.setRemainingBalance(loan.getRemainingBalance());
        dto.setMonthlyPayment(loan.getMonthlyPayment());

        if (loan.getStatus() != null) {
            dto.setStatus(loan.getStatus().getStatusName());
        }

        dto.setNextPaymentDate(calculateNextPaymentDate(loan));
        return dto;
    }

    private TransactionSummaryDTO mapToTransactionSummaryDTO(TransactionRecord transaction) {
        TransactionSummaryDTO dto = new TransactionSummaryDTO();
        dto.setTransactionRef(transaction.getTransactionRef());

        if (transaction.getSavingsAccount() != null &&
                transaction.getSavingsAccount().getProfile() != null) {
            dto.setMemberName(
                    transaction.getSavingsAccount().getProfile().getFirstName() + " " +
                            transaction.getSavingsAccount().getProfile().getLastName()
            );
        }

        dto.setTransactionType(transaction.getTransactionType());
        dto.setAmount(transaction.getAmount());
        dto.setTransactionDate(transaction.getTransactionDate());
        return dto;
    }

    private LocalDate calculateNextPaymentDate(Loan loan) {
        List<LoanPaymentSchedule> pendingSchedule = loanPaymentScheduleRepository
                .findByLoan_LoanIdAndStatusOrderByDueDate(loan.getLoanId(), "PENDING");

        if (!pendingSchedule.isEmpty()) {
            return pendingSchedule.get(0).getDueDate();
        }
        return null;
    }

    private List<DashboardChartDTO> generateTrendData(String type) {
        List<DashboardChartDTO> trend = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusMonths(i);
            String label = date.getMonth().toString().substring(0, 3) + " " + date.getYear();

            DashboardChartDTO point = new DashboardChartDTO();
            point.setLabel(label);

            // Generate random values for demo
            if ("Savings".equals(type)) {
                point.setValue(BigDecimal.valueOf(100000 + Math.random() * 50000));
            } else {
                point.setValue(BigDecimal.valueOf(50000 + Math.random() * 30000));
            }

            trend.add(point);
        }

        return trend;
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new RuntimeException("Invalid authorization header");
    }
}