package de.matthiasfisch.planningpoker.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

@Service
class PasswordHashingService(
    @Value("\${security.password-hashing.secret}") secret: String,
    @Value("\${security.password-hashing.salt-length}") saltLength: Int,
    @Value("\${security.password-hashing.iterations}") iterations: Int,
    @Value("\${security.password-hashing.algorithm}") algorithm: SecretKeyFactoryAlgorithm
) {
    private val encoder = Pbkdf2PasswordEncoder(secret, saltLength, iterations, algorithm).apply {
        setEncodeHashAsBase64(true)
    }

    /**
     * Encodes the given plaintext to a PBKDF2 password hash.
     * @param plaintext The plaintext to encode.
     * @return Returns the PBKDF2 password hash.
     */
    fun encodePlaintext(plaintext: String) = encodeItermediate(createIntermediate(plaintext))

    /**
     * Encodes a SHA-512 encoded password to a PBKDF2 password hash.
     * @param intermediate The SHA-512 hashed password.
     * @return Returns the PBKDF2 password hash.
     */
    fun encodeItermediate(intermediate: String) = encoder.encode(intermediate)

    fun createIntermediate(plaintext: String): String = MessageDigest.getInstance("SHA-512")
        .digest(plaintext.toByteArray(StandardCharsets.UTF_8))
        .let { Base64.getEncoder().encode(it) }
        .toString(StandardCharsets.UTF_8)

    fun intermediateMatches(intermediate: String, encodedPassword: String) = encoder.matches(intermediate, encodedPassword)
}