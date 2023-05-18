package de.matthiasfisch.planningpoker.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder

class PasswordHashingServiceTest : FunSpec() {
    init {
        val subject = PasswordHashingService(
            secret = "super-secret-secret",
            saltLength = 10,
            iterations = 30000,
            Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256
        )

        context("intermediateMatches") {
            test("Returns true for same passwords") {
                // Arrange

                // Act
                val result = subject.intermediateMatches(
                    intermediate = subject.createIntermediate("test"),
                    encodedPassword = subject.encodePlaintext("test")
                )

                // Assert
                result shouldBe true
            }

            test("Returns false for different passwords") {
                // Arrange

                // Act
                val result = subject.intermediateMatches(
                    intermediate = subject.createIntermediate("other"),
                    encodedPassword = subject.encodePlaintext("test")
                )

                // Assert
                result shouldBe false
            }
        }
    }
}
