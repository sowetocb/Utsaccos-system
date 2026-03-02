package com.saccos_system.controller;

import com.saccos_system.dto.StatementDTO.StatementRequestDTO;
import com.saccos_system.dto.StatementDTO.StatementResponseDTO;
import com.saccos_system.dto.StatementDTO.StatementSummaryDTO;
import com.saccos_system.service.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
@Tag(name = "Statements", description = "Monthly and annual statements")
@SecurityRequirement(name = "bearerAuth")
public class StatementController {

    private final StatementService statementService;


    @GetMapping
    @Operation(summary = "Get statement history")
    public ResponseEntity<List<StatementSummaryDTO>> getStatementHistory(
            @RequestHeader("Authorization") String token) {
        List<StatementSummaryDTO> statements = statementService.getStatementHistory(token);
        return ResponseEntity.ok(statements);
    }

    @GetMapping("/{statementNumber}")
    @Operation(summary = "Get statement by number")
    public ResponseEntity<StatementResponseDTO> getStatement(
            @RequestHeader("Authorization") String token,
            @PathVariable String statementNumber) {
        StatementResponseDTO statement = statementService.getStatementByNumber(token, statementNumber);
        return ResponseEntity.ok(statement);
    }



    @GetMapping("/current")
    @Operation(summary = "Get current month statement")
    public ResponseEntity<StatementResponseDTO> getCurrentStatement(
            @RequestHeader("Authorization") String token) {
        java.time.LocalDate now = java.time.LocalDate.now();
        StatementRequestDTO request = new StatementRequestDTO();
        request.setMonth(now.getMonthValue());
        request.setYear(now.getYear());

        StatementResponseDTO statement = statementService.generateMonthlyStatement(token, request);
        return ResponseEntity.ok(statement);
    }
}