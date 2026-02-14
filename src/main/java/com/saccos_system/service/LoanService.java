package com.saccos_system.service;


import com.saccos_system.dto.*;
import com.saccos_system.model.*;
import com.saccos_system.repository.*;
import com.saccos_system.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final NotificationService notificationService;

    public LoanApplicationResponseDTO applyForLoan(String token, LoanApplicationDTO request) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        // Check if user has active savings account
        SavingsAccount savingsAccount = savingsAccountRepository
                .findByProfile_ProfileId(user.getProfile().getProfileId())
                .orElseThrow(() -> new RuntimeException("No active savings account found"));

        // Check minimum savings balance requirement
        BigDecimal minimumSavings = BigDecimal.valueOf(5000);
        if (savingsAccount.getBalance().compareTo(minimumSavings) < 0) {
            throw new RuntimeException("Minimum savings balance of " + minimumSavings + " required");
        }

        // Check if user has existing active loans
        List<Loan> activeLoans = loanRepository
                .findByProfile_ProfileIdAndStatus_StatusCode(user.getProfile().getProfileId(), "LOAN_ACTIVE");

        if (!activeLoans.isEmpty()) {
            throw new RuntimeException("You have existing active loans");
        }

        // Check loan eligibility (3 times savings balance)
        BigDecimal maxEligibleAmount = savingsAccount.getBalance().multiply(BigDecimal.valueOf(3));
        if (request.getAmount().compareTo(maxEligibleAmount) > 0) {
            throw new RuntimeException("Maximum eligible loan amount is " + maxEligibleAmount);
        }

        // Generate application number
        String applicationNumber = "LAPP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Get pending status
        LookupStatus pendingStatus = new LookupStatus();
        pendingStatus.setStatusId(8); // Assuming 8 is LOAN_PENDING status ID

        // Create loan application
        LoanApplication application = new LoanApplication();
        application.setApplicationNumber(applicationNumber);
        application.setProfile(user.getProfile());
        application.setLoanType(request.getLoanType());
        application.setAmount(request.getAmount());
        application.setTermMonths(request.getTermMonths());
        application.setPurpose(request.getPurpose());
        application.setStatus(pendingStatus);
        application.setCreatedBy(user.getUsername());

        LoanApplication savedApplication = loanApplicationRepository.save(application);

        // Send notification
        notificationService.sendLoanApplicationNotification(user, savedApplication);

        return mapToApplicationResponse(savedApplication);
    }

    public List<LoanApplicationResponseDTO> getMyLoanApplications(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        List<LoanApplication> applications = loanApplicationRepository
                .findByProfile_ProfileIdOrderByAppliedDateDesc(user.getProfile().getProfileId());

        return applications.stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
    }

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

    public List<LoanSummaryDTO> getMyLoans(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        SystemUser user = getUser(userId);

        List<Loan> loans = loanRepository.findByProfile_ProfileId(user.getProfile().getProfileId());

        return loans.stream()
                .map(this::mapToLoanSummary)
                .collect(Collectors.toList());
    }

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

        // Check minimum payment
        BigDecimal minimumPayment = loan.getMonthlyPayment().multiply(BigDecimal.valueOf(0.5));
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

        LoanPayment savedPayment = paymentRepository.save(payment);

        // Update loan remaining balance
        BigDecimal newRemainingBalance = loan.getRemainingBalance().subtract(paymentAmount);
        loan.setRemainingBalance(newRemainingBalance);

        // Check if loan is fully paid
        if (newRemainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            // Get settled status
            LookupStatus settledStatus = new LookupStatus();
            settledStatus.setStatusId(13); // Assuming 13 is LOAN_SETTLED status ID
            loan.setStatus(settledStatus);
        }

        loanRepository.save(loan);

        // Update payment schedule if applicable
        updatePaymentSchedule(loan, paymentAmount);

        // Send notification
        notificationService.sendLoanPaymentNotification(user, loan, paymentAmount);

        return mapToPaymentResponse(savedPayment);
    }

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

    // Helper methods
    private SystemUser getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
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

    // Mapping methods
    private LoanApplicationResponseDTO mapToApplicationResponse(LoanApplication application) {
        LoanApplicationResponseDTO dto = new LoanApplicationResponseDTO();
        dto.setApplicationNumber(application.getApplicationNumber());
        dto.setLoanType(application.getLoanType());
        dto.setAmount(application.getAmount());
        dto.setTermMonths(application.getTermMonths());
        dto.setPurpose(application.getPurpose());
        dto.setAppliedDate(application.getAppliedDate());
        dto.setStatus(application.getStatus().getStatusName());
        dto.setApprovedAmount(application.getApprovedAmount());
        dto.setRejectionReason(application.getRejectionReason());
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
        dto.setStatus(loan.getStatus().getStatusName());

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
        dto.setStatus(loan.getStatus().getStatusName());
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

