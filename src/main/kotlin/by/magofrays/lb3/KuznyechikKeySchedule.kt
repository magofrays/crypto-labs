package by.magofrays.lb3

import kotlin.experimental.xor

fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

fun hexToBytes(hexStr: String): ByteArray {
    require(hexStr.length % 2 == 0) { "Hex string must have even length: ${hexStr.length}" }
    return ByteArray(hexStr.length / 2) {
        hexStr.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

fun ByteArray.xorWith(other: ByteArray): ByteArray {
    require(size == other.size) { "Byte arrays must have equal length: $size vs ${other.size}" }
    return ByteArray(size) { this[it] xor other[it] }
}

fun ByteArray.reversed(): ByteArray = reversedArray()

val KUZNYECHIK_PI = intArrayOf(
    252, 238, 221, 17, 207, 110, 49, 22, 251, 196, 250, 218, 35, 197, 4, 77,
    233, 119, 240, 219, 147, 46, 153, 186, 23, 54, 241, 187, 20, 205, 95, 193,
    249, 24, 101, 90, 226, 92, 239, 33, 129, 28, 60, 66, 139, 1, 142, 79,
    5, 132, 2, 174, 227, 106, 143, 160, 6, 11, 237, 152, 127, 212, 211, 31,
    235, 52, 44, 81, 234, 200, 72, 171, 242, 42, 104, 162, 253, 58, 206, 204,
    181, 112, 14, 86, 8, 12, 118, 18, 191, 114, 19, 71, 156, 183, 93, 135,
    21, 161, 150, 41, 16, 123, 154, 199, 243, 145, 120, 111, 157, 158, 178, 177,
    50, 117, 25, 61, 255, 53, 138, 126, 109, 84, 198, 128, 195, 189, 13, 87,
    223, 245, 36, 169, 62, 168, 67, 201, 215, 121, 214, 246, 124, 34, 185, 3,
    224, 15, 236, 222, 122, 148, 176, 188, 220, 232, 40, 80, 78, 51, 10, 74,
    167, 151, 96, 115, 30, 0, 98, 68, 26, 184, 56, 130, 100, 159, 38, 65,
    173, 69, 70, 146, 39, 94, 85, 47, 140, 163, 165, 125, 105, 213, 149, 59,
    7, 88, 179, 64, 134, 172, 29, 247, 48, 55, 107, 228, 136, 217, 231, 137,
    225, 27, 131, 73, 76, 63, 248, 254, 141, 83, 170, 144, 202, 216, 133, 97,
    32, 113, 103, 164, 45, 43, 9, 91, 203, 155, 37, 208, 190, 229, 108, 82,
    89, 166, 116, 210, 230, 244, 180, 192, 209, 102, 175, 194, 57, 75, 99, 182
)

val LINEAR_VECTOR = intArrayOf(148, 32, 133, 16, 194, 192, 1, 251, 1, 192, 194, 16, 133, 32, 148, 1)

fun gfMul(a: Int, b: Int): Int {
    var res = 0
    var x = a and 0xFF
    var y = b and 0xFF
    while (y != 0) {
        if (y and 1 != 0) res = res xor x
        val hi = x and 0x80
        x = (x shl 1) and 0xFF
        if (hi != 0) x = x xor 0xC3
        y = y shr 1
    }
    return res
}

fun substitute(block: ByteArray): ByteArray =
    ByteArray(block.size) { KUZNYECHIK_PI[block[it].toInt() and 0xFF].toByte() }

fun rTransform(block: ByteArray): ByteArray {
    require(block.size == 16) { "R expects 16 bytes" }
    var acc = 0
    for (i in block.indices) acc = acc xor gfMul(LINEAR_VECTOR[i], block[i].toInt() and 0xFF)
    return byteArrayOf(acc.toByte()) + block.copyOfRange(0, block.size - 1)
}

fun lTransform(block: ByteArray): ByteArray {
    var out = block
    repeat(16) { out = rTransform(out) }
    return out
}

fun iterationConstants(): List<ByteArray> =
    (1..32).map { lTransform(hexToBytes("%032x".format(it))) }

data class KeyScheduleResult(
    val roundKeys: List<ByteArray>,
    val feistelLog: List<FeistelStep>
)

data class FeistelStep(
    val iteration: Int,
    val temp: ByteArray,
    val newKey1: ByteArray,
    val newKey2: ByteArray
)

fun expandKey(key: ByteArray): KeyScheduleResult {
    require(key.size == 32) { "Kuznyechik key must be 32 bytes" }
    var k1 = key.copyOfRange(0, 16)
    var k2 = key.copyOfRange(16, 32)
    val roundKeys = mutableListOf(k1, k2)
    val log = mutableListOf<FeistelStep>()
    val constants = iterationConstants()
    for (i in 0 until 32) {
        val temp = lTransform(substitute(k1.xorWith(constants[i])))
        val newK1 = temp.xorWith(k2)
        val newK2 = k1
        k1 = newK1
        k2 = newK2
        log.add(FeistelStep(i + 1, temp, k1, k2))
        if ((i + 1) % 8 == 0) {
            roundKeys.add(k1)
            roundKeys.add(k2)
        }
    }
    return KeyScheduleResult(roundKeys, log)
}

fun encryptBlock(block: ByteArray, keys: List<ByteArray>): ByteArray {
    require(block.size == 16) { "Kuznyechik block must be 16 bytes" }
    require(keys.size >= 10) { "Kuznyechik needs at least 10 round keys" }
    var out = block
    for (i in 0 until 9) out = lTransform(substitute(out.xorWith(keys[i])))
    return out.xorWith(keys[9])
}

fun main() {
    println("=== 1. Verification against the GOST R 34.12-2015 reference (RFC 7801) ===")
    val gostKey = hexToBytes("8899aabbccddeeff0011223344556677fedcba98765432100123456789abcdef")
    val gostPt = hexToBytes("1122334455667700ffeeddccbbaa9988")
    val gostCtExpected = "7f679d90bebc24305a468d42b9d4edcd"
    val gostSchedule = expandKey(gostKey)
    val gostCt = encryptBlock(gostPt, gostSchedule.roundKeys)
    println("   Plaintext  : ${gostPt.toHex()}")
    println("   Ciphertext : ${gostCt.toHex()}")
    println("   Reference  : $gostCtExpected")
    println("   Match      : ${if (gostCt.toHex().equals(gostCtExpected, ignoreCase = true)) "YES" else "NO"}\n")

    println("=== 2. Iteration constants C_i = L(Vec_i) ===")
    val constants = iterationConstants()
    val articleC = mapOf(
        1 to "019484DD10BD275DB87A486C7276A2E6",
        19 to "132BD8EAF3855C7EDA22D2BBBAF6979D"
    )
    println("   C1  (computed, GOST order)        = ${constants[0].toHex().uppercase()}")
    println("   C1  (computed, article byte order) = ${constants[0].reversed().toHex().uppercase()}")
    println("   C1  (article reference)            = ${articleC[1]}")
    println("   Note: in the article the last byte of C1 is E6, per GOST it is 6E")
    println("   (typo in the article; all other bytes and C2..C32 match)\n")

    println("=== 3. Iteration key material: C_{N+2}, N = 17 -> C19 ===")
    val c19 = constants[18]
    println("   Vec19 = 00000000000000000000000000000013  (19 = 0x13)")
    println("   C19 = L(Vec19) = ${c19.toHex().uppercase()}")
    println("   C19 in article byte order = ${c19.reversed().toHex().uppercase()}")
    println("   Article reference         = ${articleC[19]}")
    println("   Match: ${if (c19.reversed().toHex().uppercase() == articleC[19]) "YES" else "NO"}\n")

    println("=== 4. Round-key schedule against the article (Habr 459004) ===")
    val articleKeyHex = "7766554433221100FFEEDDCCBBAA9988EFCDAB89674523011032547698BADCFE"
    val key = hexToBytes(articleKeyHex.substring(0, 32)).reversed() +
            hexToBytes(articleKeyHex.substring(32)).reversed()
    val schedule = expandKey(key)

    val articleEtalon = listOf(
        "7766554433221100FFEEDDCCBBAA9988", "EFCDAB89674523011032547698BADCFE",
        "448CC78CEF6A8D2243436915534831DB", "04FD9F0AC4ADEB1568ECCFE9D853453D",
        "ACF129F44692E5D3285E4AC468646457", "1B58DA3428E832B532645C16359407BD",
        "B198005A26275770DE45877E7540E651", "84F98622A2912AD73EDD9F7B0125795A",
        "17E5B6CD732FF3A52331C77853E244BB", "43404A8EA8BA5D755BF4BC1674DDE972"
    )
    println("   Article key: $articleKeyHex")
    println("   (reversed byte order = GOST R 34.12-2015 reference key)\n")
    var allOk = true
    for (i in 0 until 10) {
        val mineArticleOrder = schedule.roundKeys[i].reversed().toHex().uppercase()
        val ok = mineArticleOrder == articleEtalon[i]
        if (!ok) allOk = false
        println("   K%-2d computed = %s   reference = %s  %s".format(
            i + 1, mineArticleOrder, articleEtalon[i], if (ok) "MATCH" else "MISMATCH"
        ))
    }
    println("   Comparison result: ${if (allOk) "ALL 10 KEYS MATCH" else "MISMATCHES FOUND"}\n")

    println("=== 5. Feistel iteration No. 19 (material C19) ===")
    val iter19 = schedule.feistelLog.first { it.iteration == 19 }
    println("   temp = L(S(X(K1, C19))) = ${iter19.temp.toHex().uppercase()}")
    println("   New pair: K1' = ${iter19.newKey1.toHex().uppercase()}")
    println("             K2' = ${iter19.newKey2.toHex().uppercase()}")
    println("   In article byte order:")
    println("   temp  = ${iter19.temp.reversed().toHex().uppercase()}")
    println("   K1'   = ${iter19.newKey1.reversed().toHex().uppercase()}")
    println("   K2'   = ${iter19.newKey2.reversed().toHex().uppercase()}\n")

    println("=== 6. Encryption of the article block ===")
    val articlePt = hexToBytes("8899AABBCCDDEEFF0077665544332211")
    val ct = encryptBlock(articlePt.reversed(), schedule.roundKeys)
    println("   T = 8899AABBCCDDEEFF0077665544332211")
    println("   Ciphertext (article order) = ${ct.reversed().toHex().uppercase()}")
    println("   Article reference (T10)    = CDEDD4B9428D465A3024BCBE909D677F")
    println("   Match: ${if (ct.reversed().toHex().uppercase() == "CDEDD4B9428D465A3024BCBE909D677F") "YES" else "NO"}")
}