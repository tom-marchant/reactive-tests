package com.finchindustries.reactive

import org.junit.Test
import org.reactivestreams.Subscription
import reactor.core.publisher.BaseSubscriber
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import java.time.Duration

class ReactiveTests {
    @Test
    fun fluxes() {
        val ints = Flux.range(1, 10)
                .map { i ->
                    if (i <= 100) i else throw RuntimeException("Got to 4")
                }

        ints.subscribe(
                { i: Int? -> println(i) },
                { error: Throwable -> System.err.println("Error: $error") },
                { println("I'm done") },
                { subscription: Subscription -> subscription.request(9) })
    }

    @Test
    fun intervalFlux() {
        val flux: Flux<Long> = Flux.interval(Duration.ofSeconds(1))
                .flatMap { tick: Long ->
                    println("From publisher: $tick")
                    Mono.just(tick)
                }
                .flatMap { tick -> Flux.fromArray(arrayOf(tick)) }

        flux.subscribe(
                { i: Long? -> println("Subscriber 1: $i") },
                { error: Throwable -> System.err.println("Error: $error") },
                { println("I'm done") },
                { subscription: Subscription -> subscription.request(2) })

        flux.subscribe(
                { i: Long? -> println("Subscriber 2: $i") },
                { error: Throwable -> System.err.println("Error: $error") },
                { println("I'm done") },
                { subscription: Subscription -> subscription.cancel() })

        Thread.sleep(10000)
    }

    @Test
    fun baseSubscriber() {
        val ints = Flux.range(1, 10)
                .map { i ->
                    if (i <= 100) i else throw RuntimeException("Got to 4")
                }

        ints.subscribe(SampleSubscriber())
    }

    @Test
    fun parallel() {
        val s = Schedulers.newParallel("parallel-scheduler", 4)

        val flux = Flux
                .range(1, 10)
                .map { i -> 10 + i }
                .publishOn(s)
                .map { i -> "value $i, Thread: " + Thread.currentThread().name }

        Thread { flux.subscribe { println(Thread.currentThread().name + " -> " + it) } }.run()

        Thread.sleep(2000)
    }

    @Test
    fun virtualTime() {
        StepVerifier.withVirtualTime { Mono.delay(Duration.ofDays(1)) }
                .expectSubscription()
                .expectNoEvent(Duration.ofDays(1))
                .expectNext(0L)
                .verifyComplete()
    }
}

class SampleSubscriber<Int> : BaseSubscriber<Int>() {
    override fun hookOnComplete() {
        println("Done")
    }

    override fun hookOnNext(value: Int) {
        println("Sample subscriber: $value")
        request(1)
    }

    override fun hookOnSubscribe(subscription: Subscription) {
        subscription.request(1)
    }
}