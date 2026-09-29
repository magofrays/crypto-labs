package by.magofrays



fun main() {
    val plain = byteArrayOf(
        75, 73, 76, 73, 77, 81, 74, 78, 77, 75, 77
    )

    val k1 = 3
    val k2 = 1

    val cipher = encrypt(plain, k1, k2)

    println("Открытый текст:")
    println("P = ${plain.toList()}")

    println("Зашифрованный текст:")
    println("C = ${cipher.toList()}")

    val decrypted = decrypt(cipher, k1, k2)

    println("Расшифрованный текст:")
    println("D = ${decrypted.toList()}")


    check(decrypted.contentEquals(plain))
}

fun getZigZagRowIndex(col: Int, row: Int): Int {
    val period = 2 * (row - 1)
    if (period == 0) {
        return 0
    }
    val pos = col % period
    return if (pos < row) pos else period - pos
}

fun createNullTable(rows: Int, cols: Int) = MutableList(rows) {
    MutableList<Byte?>(cols) { null }
}

fun encrypt(data: ByteArray, rows: Int, shift: Int): ByteArray {
    if (rows < 1 || shift < 0) {
        throw RuntimeException("$rows < 1 || $shift < 0")
    }
    val width = data.size + shift
    val table = createNullTable(rows, width)
    data.forEachIndexed { index, byte ->
        val col = index + shift
        val row = getZigZagRowIndex(col, rows)
        table[row][col] = byte
    }
    return table
        .flatten()
        .filterNotNull()
        .toByteArray()
}

fun decrypt(data: ByteArray, rows: Int, shift: Int): ByteArray {
    if (rows < 1 || shift < 0) {
        throw RuntimeException("$rows < 1 || $shift < 0")
    }
    val width = data.size + shift
    val cells = mutableListOf<Pair<Int, Int>>()
    for (col in shift until width) {
        val row = getZigZagRowIndex(col, rows)
        cells.add(row to col)
    }
    cells.sortWith(compareBy({ it.first }, { it.second }))
    val result = ByteArray(data.size)

    for (i in data.indices) {
        val col = cells[i].second
        result[col - shift] = data[i]
    }
    return result
}