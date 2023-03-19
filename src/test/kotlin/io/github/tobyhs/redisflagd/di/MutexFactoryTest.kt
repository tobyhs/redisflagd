package io.github.tobyhs.redisflagd.di

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.sync.Mutex

class MutexFactoryTest : DescribeSpec({
    describe("mutex") {
        it("returns a Mutex") {
            val mutex = MutexFactory().mutex()
            mutex::class.shouldBe(Mutex()::class)
        }
    }
})
