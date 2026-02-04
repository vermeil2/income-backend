package com.example.tossbackend.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

@Schema(description = "세금 환급 계산 요청 (1년치 총 소득·지출)")
data class TaxCalculateRequest(
    @JsonProperty("annualIncome")
    @field:Min(0, message = "annualIncome must be >= 0")
    @Schema(description = "연간 총 소득 (원)", example = "50000000", required = true)
    val annualIncome: Long,

    @JsonProperty("annualExpenses")
    @field:Min(0, message = "annualExpenses must be >= 0")
    @Schema(description = "연간 총 지출 (원)", example = "15000000", required = true)
    val annualExpenses: Long
)
