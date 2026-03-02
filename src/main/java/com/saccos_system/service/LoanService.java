package com.saccos_system.service;

import com.saccos_system.dto.AdminDTO.ScheduleItemDTO;
import com.saccos_system.dto.LoanDTO.*;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanService {

    private final JwtTokenUtil jwtTokenUtil;
    private final SystemUserRepository userRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepository loanRepository;
    private final LoanPaymentScheduleRepository scheduleRepository;
    private final LoanPaymentRepository paymentRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LookupStatusRepository lookupStatusRepository;
    private final TransactionRecordRepository transactionRepository;
    private final NotificationService notificationService;

    /**
     * Apply for a loan (regular or emergency)
     */
    @Transactional
    public LoanApplicationResponseDTO applyForLoan(String token, LoanApplicationDTO request) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        SavingsAccount savingsAccount = getSavingsAccount(user);
        LoanType loanType = request.getLoanType();

        // Validate loan eligibility based on loan type
        validateLoanEligibility(user, savingsAccount, request, loanType);

        // Generate application number
        String applicationNumber = generateApplicationNumber(loanType);

        // Determine initial status based on loan type
        LookupStatus initialStatus = getInitialStatus(loanType);

        // Create loan application
        LoanApplication application = createLoanApplication(request, user, applicationNumber, initialStatus);

        // Set emergency-specific fields
        if (loanType == LoanType.EMERGENCY) {
            application.setEmergencyReason(request.getEmergencyReason());
            application.setSupportingDocument(request.getSupportingDocument());
            application.setDocumentVerified(false);
            application.setPhoneVerified(false);
        }

        LoanApplication savedApplication = loanApplicationRepository.save(application);

        // Send appropriate notifications
        sendApplicationNotifications(user, savedApplication, loanType);

        log.info("User {} applied for {} loan: {}", user.getUsername(), loanType, applicationNumber);

        return mapToApplicationResponse(savedApplication);
    }

    /**
     * Validate loan eligibility based on loan type
     */
    private void validateLoanEligibility(SystemUser user, SavingsAccount savingsAccount,
                                         LoanApplicationDTO request, LoanType loanType) {

        // Check minimum savings balance based on loan type
        BigDecimal minimumSavings = loanType == LoanType.EMERGENCY ?
                BigDecimal.valueOf(2000) : BigDecimal.valueOf(5000);

        if (savingsAccount.getBalance().compareTo(minimumSavings) < 0) {
            throw new RuntimeException(
                    String.format("Minimum savings balance of %,.2f required for %s loan",
                            minimumSavings, loanType.getDisplayName()));
        }

        // Calculate maximum eligible amount based on loan type multiplier
        BigDecimal maxEligibleAmount = savingsAccount.getBalance()
                .multiply(BigDecimal.valueOf(loanType.getSavingsMultiplier()));

        if (request.getAmount().compareTo(maxEligibleAmount) > 0) {
            throw new RuntimeException(
                    String.format("Maximum eligible %s loan amount is %,.2f (%.1fx your savings)",
                            loanType.getDisplayName(), maxEligibleAmount, loanType.getSavingsMultiplier()));
        }

        // Check term limits
        if (loanType == LoanType.EMERGENCY && request.getTermMonths() > 6) {
            throw new RuntimeException("Emergency loans cannot exceed 6 months term");
        }

        if (loanType != LoanType.EMERGENCY && request.getTermMonths() > 60) {
            throw new RuntimeException("Regular loans cannot exceed 60 months term");
        }

        // For non-emergency loans, check for existing active loans
        if (loanType != LoanType.EMERGENCY) {
            List<Loan> activeLoans = loanRepository
                    .findByProfile_ProfileIdAndStatus_StatusCode(
                            user.getProfile().getProfileId(), "LOAN_ACTIVE");

            if (!activeLoans.isEmpty()) {
                throw new RuntimeException(
                        "You have existing active loans. Please complete them before applying for a new " +
                                loanType.getDisplayName() + " loan.");
            }
        }

        // Validate emergency-specific requirements
        if (loanType == LoanType.EMERGENCY) {
            if (request.getEmergencyReason() == null || request.getEmergencyReason().isEmpty()) {
                throw new RuntimeException("Emergency reason is required for emergency loans");
            }

            // Check if user has had an emergency loan in the last 6 months
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            List<LoanApplication> recentEmergencyLoans = loanApplicationRepository
                    .findByProfile_ProfileIdAndLoanTypeAndAppliedDateAfter(
                            user.getProfile().getProfileId(), "EMERGENCY", sixMonthsAgo);

            if (!recentEmergencyLoans.isEmpty()) {
                throw new RuntimeException(
                        "You have already taken an emergency loan in the last 6 months. " +
                                "Emergency loans are limited to one every 6 months.");
            }
        }
    }

    /**
     * Get initial status based on loan type
     */
    private LookupStatus getInitialStatus(LoanType loanType) {
        String statusCode = loanType == LoanType.EMERGENCY ?
                "LOAN_PENDING_EMERGENCY" : "LOAN_PENDING";

        return lookupStatusRepository.findByStatusCode(statusCode)
                .orElseThrow(() -> new RuntimeException(
                        loanType == LoanType.EMERGENCY ?
                                "Emergency pending status not found" : "Pending status not found"));
    }

    /**
     * Create loan application entity
     */
    private LoanApplication createLoanApplication(LoanApplicationDTO request, SystemUser user,
                                                  String applicationNumber, LookupStatus status) {
        LoanApplication application = new LoanApplication();
        application.setApplicationNumber(applicationNumber);
        application.setProfile(user.getProfile());
        application.setLoanType(request.getLoanType().name());
        application.setAmount(request.getAmount());
        application.setTermMonths(request.getTermMonths());
        application.setPurpose(request.getPurpose());
        application.setStatus(status);
        application.setCreatedBy(user.getUsername());
        application.setAppliedDate(LocalDateTime.now());

        // Set guarantor information for regular loans
        if (request.getLoanType() != LoanType.EMERGENCY) {
            application.setGuarantorIdNumber(request.getGuarantorIdNumber());
            application.setGuarantorName(request.getGuarantorName());
            application.setGuarantorPhone(request.getGuarantorPhone());
        }

        return application;
    }

    /**
     * Send appropriate notifications based on loan type
     */
    private void sendApplicationNotifications(SystemUser user, LoanApplication application, LoanType loanType) {
        if (loanType == LoanType.EMERGENCY) {
            // Send URGENT notification to admins and loan officers
            //notificationService.sendEmergencyLoanAlertToAdmins(application);
            // Send acknowledgment to member
            //notificationService.sendEmergencyLoanAcknowledgment(user, application);
            log.info("Emergency loan alert sent for application: {}", application.getApplicationNumber());
        } else {
            notificationService.sendLoanApplicationNotification(user, application);
        }
    }

    /**
     * Generate application number with appropriate prefix
     */
    private String generateApplicationNumber(LoanType loanType) {
        String prefix = loanType == LoanType.EMERGENCY ? "EMG" : "LAPP";
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Get all loan applications for the current user
     */
    public List<LoanApplicationResponseDTO> getMyLoanApplications(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        List<LoanApplication> applications = loanApplicationRepository
                .findByProfile_ProfileIdOrderByAppliedDateDesc(user.getProfile().getProfileId());

        return applications.stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get loan details by loan number
     */
    public LoanDetailsDTO getLoanDetails(String token, String loanNumber) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        Loan loan = loanRepository.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Verify loan belongs to user
        if (!loan.getProfile().getProfileId().equals(user.getProfile().getProfileId())) {
            throw new RuntimeException("Unauthorized access to loan details");
        }

        return mapToLoanDetails(loan);
    }

    /**
     * Get all loans for the current user
     */
    public List<LoanSummaryDTO> getMyLoans(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        List<Loan> loans = loanRepository.findByProfile_ProfileId(user.getProfile().getProfileId());

        return loans.stream()
                .map(this::mapToLoanSummary)
                .collect(Collectors.toList());
    }

    /**
     * Make a loan payment
     */
    public LoanPaymentResponseDTO makePayment(String token, LoanPaymentDTO request) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        Loan loan = loanRepository.findByLoanNumber(request.getLoanNumber())
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Verify loan belongs to user
        if (!loan.getProfile().getProfileId().equals(user.getProfile().getProfileId())) {
            throw new RuntimeException("Unauthorized payment");
        }

        // Check if loan is active
        if (!"LOAN_ACTIVE".equals(loan.getStatus().getStatusCode())) {
            throw new RuntimeException("Loan is not active for payments");
        }

        BigDecimal paymentAmount = request.getAmount();

        // If paying full balance, set amount to remaining balance
        if (Boolean.TRUE.equals(request.getPayFullBalance())) {
            paymentAmount = loan.getRemainingBalance();
        }

        // Check minimum payment (different for emergency loans)
        BigDecimal minimumPayment = "EMERGENCY".equals(loan.getLoanType()) ?
                loan.getMonthlyPayment() : // Emergency loans require full monthly payment
                loan.getMonthlyPayment().multiply(BigDecimal.valueOf(0.5)); // Regular loans allow half

        if (paymentAmount.compareTo(minimumPayment) < 0) {
            throw new RuntimeException("Minimum payment amount is " + minimumPayment);
        }

        // Check if payment exceeds remaining balance
        if (paymentAmount.compareTo(loan.getRemainingBalance()) > 0) {
            throw new RuntimeException("Payment amount exceeds remaining balance");
        }

        // Generate payment reference
        String paymentReference = "PAY" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create payment record
        LoanPayment payment = new LoanPayment();
        payment.setPaymentReference(paymentReference);
        payment.setLoan(loan);
        payment.setAmount(paymentAmount);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionReference(request.getTransactionReference());
        payment.setPaidBy(user.getUsername());
        payment.setNotes(request.getNotes());
        payment.setCreatedBy(user.getUsername());
        payment.setPaymentDate(LocalDateTime.now());

        LoanPayment savedPayment = paymentRepository.save(payment);

        // Update loan remaining balance
        BigDecimal newRemainingBalance = loan.getRemainingBalance().subtract(paymentAmount);
        loan.setRemainingBalance(newRemainingBalance);

        // Check if loan is fully paid
        if (newRemainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            LookupStatus settledStatus = lookupStatusRepository.findByStatusCode("LOAN_SETTLED")
                    .orElseThrow(() -> new RuntimeException("Settled status not found"));
            loan.setStatus(settledStatus);
        }

        loanRepository.save(loan);

        // Update payment schedule
        updatePaymentSchedule(loan, paymentAmount);

        // Send notification
        notificationService.sendLoanPaymentNotification(user, loan, paymentAmount);

        log.info("Payment of {} received for loan {} by user {}",
                paymentAmount, loan.getLoanNumber(), user.getUsername());

        return mapToPaymentResponse(savedPayment);
    }

    /**
     * Get payment history for a loan
     */
    public List<LoanPaymentResponseDTO> getPaymentHistory(String token, String loanNumber) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        Loan loan = loanRepository.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Verify loan belongs to user
        if (!loan.getProfile().getProfileId().equals(user.getProfile().getProfileId())) {
            throw new RuntimeException("Unauthorized access");
        }

        List<LoanPayment> payments = paymentRepository.findByLoan_LoanIdOrderByPaymentDateDesc(loan.getLoanId());

        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get payment schedule for a loan
     */
    public LoanScheduleDTO getPaymentSchedule(String token, String loanNumber) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        Loan loan = loanRepository.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Verify loan belongs to user
        if (!loan.getProfile().getProfileId().equals(user.getProfile().getProfileId())) {
            throw new RuntimeException("Unauthorized access");
        }

        List<LoanPaymentSchedule> schedule = scheduleRepository
                .findByLoan_LoanIdOrderByDueDate(loan.getLoanId());

        LoanScheduleDTO scheduleDTO = new LoanScheduleDTO();
        scheduleDTO.setLoanNumber(loan.getLoanNumber());
        scheduleDTO.setTotalInstallments(schedule.size());

        List<ScheduleItemDTO> scheduleItems = schedule.stream()
                .map(this::mapToScheduleItem)
                .collect(Collectors.toList());

        scheduleDTO.setSchedule(scheduleItems);

        // Calculate summary
        BigDecimal totalPaid = scheduleItems.stream()
                .filter(item -> "PAID".equals(item.getStatus()))
                .map(ScheduleItemDTO::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        scheduleDTO.setTotalPaid(totalPaid);
        scheduleDTO.setTotalDue(loan.getPrincipalAmount()
                .add(calculateTotalInterest(loan)));
        scheduleDTO.setRemainingBalance(loan.getRemainingBalance());

        return scheduleDTO;
    }

    // ========== HELPER METHODS ==========

    private SystemUser getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private SavingsAccount getSavingsAccount(SystemUser user) {
        return savingsAccountRepository.findByProfile_ProfileId(user.getProfile().getProfileId())
                .orElseThrow(() -> new RuntimeException("Savings account not found"));
    }

    private void updatePaymentSchedule(Loan loan, BigDecimal paymentAmount) {
        List<LoanPaymentSchedule> pendingSchedule = scheduleRepository
                .findByLoan_LoanIdAndStatusOrderByDueDate(loan.getLoanId(), "PENDING");

        BigDecimal remainingPayment = paymentAmount;

        for (LoanPaymentSchedule schedule : pendingSchedule) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal amountDue = schedule.getAmountDue();

            if (remainingPayment.compareTo(amountDue) >= 0) {
                // Full payment for this installment
                schedule.setPaidAmount(amountDue);
                schedule.setPaidDate(LocalDate.now());
                schedule.setStatus("PAID");
                remainingPayment = remainingPayment.subtract(amountDue);
            } else {
                // Partial payment
                schedule.setPaidAmount(remainingPayment);
                schedule.setStatus("PARTIAL");
                remainingPayment = BigDecimal.ZERO;
            }

            scheduleRepository.save(schedule);
        }
    }

    private BigDecimal calculateTotalInterest(Loan loan) {
        return loan.getPrincipalAmount()
                .multiply(loan.getInterestRate())
                .divide(BigDecimal.valueOf(100))
                .multiply(BigDecimal.valueOf(loan.getTermMonths()))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    // ========== MAPPING METHODS ==========

    private LoanApplicationResponseDTO mapToApplicationResponse(LoanApplication application) {
        LoanApplicationResponseDTO dto = new LoanApplicationResponseDTO();
        dto.setApplicationNumber(application.getApplicationNumber());
        dto.setLoanType(application.getLoanType());
        dto.setAmount(application.getAmount());
        dto.setTermMonths(application.getTermMonths());
        dto.setPurpose(application.getPurpose());
        dto.setAppliedDate(application.getAppliedDate());

        if (application.getStatus() != null) {
            dto.setStatus(application.getStatus().getStatusName());
        }

        dto.setApprovedAmount(application.getApprovedAmount());
        dto.setRejectionReason(application.getRejectionReason());

        // Add emergency-specific fields with null safety
        if ("EMERGENCY".equals(application.getLoanType())) {
            dto.setEmergencyReason(application.getEmergencyReason());
            dto.setDocumentVerified(application.getDocumentVerified() != null ?
                    application.getDocumentVerified() : false);
            dto.setPhoneVerified(application.getPhoneVerified() != null ?
                    application.getPhoneVerified() : false);
        }

        return dto;
    }

    private LoanDetailsDTO mapToLoanDetails(Loan loan) {
        LoanDetailsDTO dto = new LoanDetailsDTO();
        dto.setLoanNumber(loan.getLoanNumber());
        dto.setLoanType(loan.getLoanType());
        dto.setPrincipalAmount(loan.getPrincipalAmount());
        dto.setInterestRate(loan.getInterestRate());
        dto.setTermMonths(loan.getTermMonths());
        dto.setStartDate(loan.getStartDate());
        dto.setEndDate(loan.getEndDate());
        dto.setMonthlyPayment(loan.getMonthlyPayment());
        dto.setRemainingBalance(loan.getRemainingBalance());

        if (loan.getStatus() != null) {
            dto.setStatus(loan.getStatus().getStatusName());
        }

        // Calculate total paid
        List<LoanPayment> payments = paymentRepository.findByLoan_LoanIdOrderByPaymentDateDesc(loan.getLoanId());
        BigDecimal totalPaid = payments.stream()
                .map(LoanPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalPaid(totalPaid);

        return dto;
    }

    private LoanSummaryDTO mapToLoanSummary(Loan loan) {
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

    private LoanPaymentResponseDTO mapToPaymentResponse(LoanPayment payment) {
        LoanPaymentResponseDTO dto = new LoanPaymentResponseDTO();
        dto.setPaymentReference(payment.getPaymentReference());
        dto.setLoanNumber(payment.getLoan().getLoanNumber());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setTransactionReference(payment.getTransactionReference());
        return dto;
    }

    private ScheduleItemDTO mapToScheduleItem(LoanPaymentSchedule schedule) {
        ScheduleItemDTO dto = new ScheduleItemDTO();
        dto.setInstallmentNumber(schedule.getInstallmentNumber());
        dto.setDueDate(schedule.getDueDate());
        dto.setAmountDue(schedule.getAmountDue());
        dto.setPrincipalAmount(schedule.getPrincipalAmount());
        dto.setInterestAmount(schedule.getInterestAmount());
        dto.setPaidAmount(schedule.getPaidAmount());
        dto.setPaidDate(schedule.getPaidDate());
        dto.setStatus(schedule.getStatus());
        return dto;
    }

    private LocalDate calculateNextPaymentDate(Loan loan) {
        List<LoanPaymentSchedule> pendingSchedule = scheduleRepository
                .findByLoan_LoanIdAndStatusOrderByDueDate(loan.getLoanId(), "PENDING");

        if (!pendingSchedule.isEmpty()) {
            return pendingSchedule.get(0).getDueDate();
        }
        return null;
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new RuntimeException("Invalid authorization header");
    }
}