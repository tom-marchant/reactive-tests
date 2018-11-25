package com.finchindustries.reactive

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.reactive.function.client.ClientRequest.LOG_ID_ATTRIBUTE
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.ModelAndView
import reactor.core.publisher.Mono

@Controller
class DogsController {

    private val webClient = WebClient.builder()
            .baseUrl("https://dog.ceo/api/breeds/image/random")
            .filter(ExchangeFilterFunction.ofRequestProcessor { request ->
                val logId = request.attribute(LOG_ID_ATTRIBUTE).orElse("UNKNOWN")
                println("Requesting ${request.url()} with ID: " + logId)
                Mono.just(request)
            })
            .filter(ExchangeFilterFunction.ofResponseProcessor { response ->
                response.headers().asHttpHeaders().forEach { t: String?, u: MutableList<String>? ->
                    println("Response header: $t -> ${u?.joinToString(",")}")
                }
                Mono.just(response)
            })
            .build()

    @GetMapping("/")
    fun getBlocking(model: MutableMap<String, Any>): ModelAndView {

        val imageResponse: RandomImageResponse = webClient
                .get()
                .retrieve()
                .bodyToMono(RandomImageResponse::class.java)
                .block()!!

        model["imageUrl"] = imageResponse.message!!

        return ModelAndView("home", model)
    }

    @GetMapping("/reactive")
    fun getReactive(model: MutableMap<String, Any>): Mono<ModelAndView> {

        return webClient
                .get()
                .retrieve()
                .bodyToMono(RandomImageResponse::class.java)
                .flatMap { randomImageResponse ->
                    model["imageUrl"] = randomImageResponse.message!!
                    Mono.just(ModelAndView("home", model))
                }
    }
}

data class RandomImageResponse(
        var status: String? = null,
        var message: String? = null)
