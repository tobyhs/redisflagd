package io.github.tobyhs.redisflagd.di

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.SupervisorJob

class CoroutineFactoryTest : DescribeSpec({
    lateinit var factory: CoroutineFactory

    beforeEach {
        factory = CoroutineFactory()
    }

    describe("appScope") {
        it("returns a coroutine scope using SupervisorJob") {
            val scope = factory.appScope()
            scope.coroutineContext::class.shouldBe(SupervisorJob()::class)
        }
    }
})
