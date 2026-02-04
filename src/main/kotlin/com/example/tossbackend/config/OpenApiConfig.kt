package com.example.tossbackend.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI().info(
        Info()
            .title("Toss Income 모사 - 세금 환급 API")
            .version("1.0")
            .description("연소득·지출을 입력받아 환급액을 계산하는 API. Swagger UI에서 바로 Try it out으로 호출해볼 수 있습니다.")
    )
}
