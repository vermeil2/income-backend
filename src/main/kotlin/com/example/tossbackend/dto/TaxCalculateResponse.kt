package com.example.tossbackend.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "세금 환급 계산 응답")
data class TaxCalculateResponse(
    @JsonProperty("refundAmount")
    @Schema(description = "계산된 환급액 (원)", example = "750000")
    val refundAmount: Long,

    @JsonProperty("annualIncome")
    @Schema(description = "입력된 연간 소득 (원)")
    val annualIncome: Long,

    @JsonProperty("annualExpenses")
    @Schema(description = "입력된 연간 지출 (원)")
    val annualExpenses: Long
)
