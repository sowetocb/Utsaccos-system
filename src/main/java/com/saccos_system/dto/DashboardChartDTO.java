package com.saccos_system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardChartDTO {
    private String label;
    private BigDecimal value;
}
