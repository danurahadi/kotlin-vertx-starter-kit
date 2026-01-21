package com.starter.library.extension

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

import id.yoframework.core.extension.string.shaHash
import org.apache.commons.codec.binary.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
fun generateHMACSHA256(key: String?, data: String?): String {
    val hmac = "HmacSHA256"
    if (key == null || data == null) throw IllegalArgumentException()

    val hMacSHA256 = Mac.getInstance(hmac)
    val hmacKeyBytes = key.toByteArray(StandardCharsets.UTF_8)

    val secretKey = SecretKeySpec(hmacKeyBytes, hmac)
    hMacSHA256.init(secretKey)

    val dataBytes = data.toByteArray(StandardCharsets.UTF_8)
    val res = hMacSHA256.doFinal(dataBytes)

    "test".shaHash()
    return Base64.encodeBase64String(res)
}

@Throws(Exception::class)
fun encryptAes(token: String, text: String): String {
    val random = SecureRandom()

    val bytes = ByteArray(20)
    random.nextBytes(bytes)

    // Derive the key
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val spec = PBEKeySpec(token.toCharArray(), bytes, 65556, 256)

    val secretKey = factory.generateSecret(spec)
    val secret = SecretKeySpec(secretKey.encoded, "AES")

    // Encrypting the word
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secret)

    val params = cipher.parameters
    val ivBytes = params.getParameterSpec(IvParameterSpec::class.java).iv

    val encryptedTextBytes = cipher.doFinal(text.toByteArray(charset("UTF-8")))

    // Prepend salt and iv
    val buffer = ByteArray(bytes.size + ivBytes.size + encryptedTextBytes.size)

    System.arraycopy(bytes, 0, buffer, 0, bytes.size)
    System.arraycopy(ivBytes, 0, buffer, bytes.size, ivBytes.size)

    System.arraycopy(encryptedTextBytes, 0, buffer, bytes.size + ivBytes.size, encryptedTextBytes.size)
    return Base64().encodeToString(buffer)
}

@Throws(Exception::class)
fun decryptAes(token:String, encryptedText: String): String {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

    // Strip off the salt and iv
    val buffer = ByteBuffer.wrap(Base64().decode(encryptedText))

    val saltBytes = ByteArray(20)
    buffer.get(saltBytes, 0, saltBytes.size)

    val ivBytes1 = ByteArray(cipher.blockSize)
    buffer.get(ivBytes1, 0, ivBytes1.size)

    val encryptedTextBytes = ByteArray(buffer.capacity() - saltBytes.size - ivBytes1.size)
    buffer.get(encryptedTextBytes)

    // Deriving the key
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val spec = PBEKeySpec(token.toCharArray(), saltBytes, 65556, 256)

    val secretKey = factory.generateSecret(spec)
    val secret = SecretKeySpec(secretKey.encoded, "AES")

    cipher.init(Cipher.DECRYPT_MODE, secret, IvParameterSpec(ivBytes1))

    var decryptedTextBytes: ByteArray? = null
    try {
        decryptedTextBytes = cipher.doFinal(encryptedTextBytes)
    } catch (e: IllegalBlockSizeException) {
        e.printStackTrace()
    } catch (e: BadPaddingException) {
        e.printStackTrace()
    }

    return String(decryptedTextBytes!!)
}
