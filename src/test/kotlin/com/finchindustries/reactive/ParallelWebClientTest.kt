package com.finchindustries.reactive

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.junit.Rule
import org.junit.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.toMono
import reactor.test.StepVerifier

class ParallelWebClientTest {

    @Rule
    @JvmField
    val wireMockRule = WireMockRule(8089)

    private val webClient = WebClient.builder()
            .baseUrl("http://localhost:8089")
            .build()

    private fun stubEndpoint(id: String, delayInSeconds: Int = 0) {
        wireMockRule.stubFor(get(urlEqualTo("/$id"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(delayInSeconds * 1000)
                        .withBody("""{
                            "message": "$id"
                        }""")))
    }

    @Test
    fun testSingleRequest() {
        stubEndpoint("1")

        StepVerifier.create(
                webClient.get()
                        .uri("/1")
                        .retrieve()
                        .bodyToMono(MessageResponse::class.java)
                        .map { it.message }
                        .toMono())
                .expectNext("1")
                .verifyComplete()
                .also {
                    verify(getRequestedFor(urlPathEqualTo("/1")))
                }
    }

    @Test
    fun testSequentialRequests() {
        (1..3).forEach { i -> stubEndpoint("$i", 2) }

        StepVerifier.create(
                Flux.just("1", "2", "3")
                        .flatMap { value ->
                            webClient.get()
                                    .uri("/$value")
                                    .retrieve()
                                    .bodyToMono(MessageResponse::class.java)
                                    .map { it.message }
                                    .toMono()
                        })
                .expectNextMatches { it?.matches(Regex("[123]")) ?: false }
                .expectNextMatches { it?.matches(Regex("[123]")) ?: false }
                .expectNextMatches { it?.matches(Regex("[123]")) ?: false }
                .verifyComplete()
                .also {
                    verify(3, getRequestedFor(anyUrl()))

                    verify(getRequestedFor(urlPathEqualTo("/1")))
                    verify(getRequestedFor(urlPathEqualTo("/2")))
                    verify(getRequestedFor(urlPathEqualTo("/3")))
                }
    }
}

data class MessageResponse(var message: String? = null)