package com.saccos_system.config;


import com.saccos_system.domain.LookupCategory;
import com.saccos_system.domain.LookupStatus;
import com.saccos_system.repository.LookupCategoryRepository;
import com.saccos_system.repository.LookupStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final LookupCategoryRepository categoryRepository;
    private final LookupStatusRepository statusRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create lookup categories if they don't exist
        createLookupCategory("MemberStatus", "Status for SACCO members");
        createLookupCategory("AccountStatus", "Status for savings accounts");
        createLookupCategory("LoanStatus", "Status for loans");
        createLookupCategory("TransactionType", "Types of transactions");

        // Create lookup statuses
        createLookupStatus("MemberStatus", "MEMBER_ACTIVE", "Active", "Active member");
        createLookupStatus("MemberStatus", "MEMBER_INACTIVE", "Inactive", "Inactive member");
        createLookupStatus("MemberStatus", "MEMBER_SUSPENDED", "Suspended", "Suspended member");
        createLookupStatus("MemberStatus", "MEMBER_TERMINATED", "Terminated", "Terminated member");

        createLookupStatus("AccountStatus", "ACCOUNT_OPEN", "Open", "Account is open");
        createLookupStatus("AccountStatus", "ACCOUNT_CLOSED", "Closed", "Account is closed");
        createLookupStatus("AccountStatus", "ACCOUNT_DORMANT", "Dormant", "Account is dormant");

        createLookupStatus("LoanStatus", "LOAN_PENDING", "Pending", "Loan application pending");
        createLookupStatus("LoanStatus", "LOAN_APPROVED", "Approved", "Loan approved");
        createLookupStatus("LoanStatus", "LOAN_REJECTED", "Rejected", "Loan rejected");
        createLookupStatus("LoanStatus", "LOAN_DISBURSED", "Disbursed", "Loan disbursed");
        createLookupStatus("LoanStatus", "LOAN_ACTIVE", "Active", "Active loan");
        createLookupStatus("LoanStatus", "LOAN_SETTLED", "Settled", "Loan settled");
        createLookupStatus("LoanStatus", "LOAN_DEFAULTED", "Defaulted", "Loan defaulted");

        createLookupStatus("TransactionType", "TRANS_DEPOSIT", "Deposit", "Money deposit");
        createLookupStatus("TransactionType", "TRANS_WITHDRAWAL", "Withdrawal", "Money withdrawal");
        createLookupStatus("TransactionType", "TRANS_INTEREST", "Interest", "Interest accrual");
        createLookupStatus("TransactionType", "TRANS_FEE", "Fee", "Service fee");

        System.out.println("Data initialization completed!");
    }

    private void createLookupCategory(String name, String description) {
        if (!categoryRepository.findByCategoryName(name).isPresent()) {
            LookupCategory category = new LookupCategory();
            category.setCategoryName(name);
            category.setDescription(description);
            categoryRepository.save(category);
        }
    }

    private void createLookupStatus(String categoryName, String code, String name, String description) {
        categoryRepository.findByCategoryName(categoryName).ifPresent(category -> {
            if (!statusRepository.findByStatusCode(code).isPresent()) {
                LookupStatus status = new LookupStatus();
                status.setCategory(category);
                status.setStatusCode(code);
                status.setStatusName(name);
                status.setDescription(description);
                statusRepository.save(status);
            }
        });
    }
}