package com.peterlaurence.trekme.util.encrypt

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val keyValue = toByte("0161E6E45E4A09DFC14D47B41DE04006408B82C4EE087CBD710C3D5B3086FE51")
private val key = SecretKeySpec(keyValue, "AES")

private const val HEX = "0123456789ABCDEF"

private const val initVector = "encryptionIntVec"
val iv = IvParameterSpec(initVector.toByteArray())

@Throws(Exception::class)
fun String.encrypt(): String {
    val result = encrypt(toByteArray())
    return toHex(result)
}

@Throws(Exception::class)
fun String.decrypt(): String {
    val enc = toByte(this)
    val result = decrypt(enc)
    return String(result)
}

@Throws(Exception::class)
private fun encrypt(clear: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
    return cipher.doFinal(clear)
}

@Throws(Exception::class)
private fun decrypt(encrypted: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    return cipher.doFinal(encrypted)
}

fun toByte(hexString: String): ByteArray {
    val len = hexString.length / 2
    val result = ByteArray(len)
    for (i in 0 until len)
        result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2),
                16).toByte()
    return result
}

fun toHex(buf: ByteArray?): String {
    if (buf == null)
        return ""
    val result = StringBuffer(2 * buf.size)
    for (i in buf.indices) {
        appendHex(result, buf[i])
    }
    return result.toString()
}

private fun appendHex(sb: StringBuffer, b: Byte) {
    sb.append(HEX[(b.toInt() shr 4) and 0x0f]).append(HEX[b.toInt() and 0x0f])
}