package com.mathenautics.backend.application.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import tools.jackson.databind.json.JsonMapper

@TestConfiguration(proxyBeanMethods = false)
class Jackson3TestConfiguration {

    @Bean
    fun jsonMapper(): JsonMapper = JsonMapper.builder().build()
}
