
package com.saccos_system.model;

public enum LoanType {
    PERSONAL("Personal Loan", 12.0, 3.0, false),
    BUSINESS("Business Loan", 12.0, 3.0, false),
    EMERGENCY("Emergency Loan", 18.0, 2.0, true),
    EDUCATION("Education Loan", 10.0, 4.0, false),
    ASSET_FINANCING("Asset Financing", 14.0, 2.5, false);
    private final String displayName;
    private final double defaultInterestRate;
    private final double savingsMultiplier;
    private final boolean autoApprove;

    LoanType(String displayName, double defaultInterestRate, double savingsMultiplier, boolean autoApprove) {
        this.displayName = displayName;
        this.defaultInterestRate = defaultInterestRate;
        this.savingsMultiplier = savingsMultiplier;
        this.autoApprove = autoApprove;
    }

    public String getDisplayName() { return displayName; }
    public double getDefaultInterestRate() { return defaultInterestRate; }
    public double getSavingsMultiplier() { return savingsMultiplier; }
    public boolean isAutoApprove() { return autoApprove; }
}