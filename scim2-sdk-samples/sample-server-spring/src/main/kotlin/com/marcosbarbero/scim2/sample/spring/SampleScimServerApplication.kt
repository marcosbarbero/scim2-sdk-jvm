package com.marcosbarbero.scim2.sample.spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SampleScimServerApplication

fun main(args: Array<String>) {
    runApplication<SampleScimServerApplication>(*args)
}
