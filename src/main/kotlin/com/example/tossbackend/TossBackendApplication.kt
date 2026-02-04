package com.example.tossbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TossBackendApplication

fun main(args: Array<String>) {
    runApplication<TossBackendApplication>(*args)
}
