package com.example.tossbackend.service

import com.example.tossbackend.dto.TaxCalculateRequest
import com.example.tossbackend.dto.TaxCalculateResponse
import org.springframework.stereotype.Service

/**
 * 세금 환급액 계산 서비스.
 * 단순 산식: 환급액 = (지출 - 소득 * 0.2) * 0.15 (음수일 경우 0)
 */
@Service
class TaxService {

    companion object {
        private const val INCOME_RATE = 0.2
        private const val REFUND_RATE = 0.15
    }

    fun calculateRefund(request: TaxCalculateRequest): TaxCalculateResponse {
        val deductible = (request.annualIncome * INCOME_RATE).toLong()
        val refundRaw = ((request.annualExpenses - deductible) * REFUND_RATE).toLong()
        val refundAmount = maxOf(0, refundRaw)

        return TaxCalculateResponse(
            refundAmount = refundAmount,
            annualIncome = request.annualIncome,
            annualExpenses = request.annualExpenses
        )
    }
}
