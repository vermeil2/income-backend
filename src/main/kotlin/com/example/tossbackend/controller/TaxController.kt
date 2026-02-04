package com.example.tossbackend.controller

import com.example.tossbackend.dto.TaxCalculateRequest
import com.example.tossbackend.dto.TaxCalculateResponse
import com.example.tossbackend.service.TaxService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tax")
@Tag(name = "세금 환급", description = "세금 환급액 계산 API (Toss Income 모사)")
class TaxController(
    private val taxService: TaxService
) {

    @PostMapping("/calculate")
    @Operation(summary = "환급액 계산", description = "1년치 총 소득·지출을 입력하면 환급액을 계산합니다. (환급액 = (지출 - 소득×0.2) × 0.15)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "계산 성공", content = [Content(schema = Schema(implementation = TaxCalculateResponse::class))]),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 음수 입력)")
        ]
    )
    fun calculate(@Valid @RequestBody request: TaxCalculateRequest): ResponseEntity<TaxCalculateResponse> {
        val response = taxService.calculateRefund(request)
        return ResponseEntity.ok(response)
    }
}
