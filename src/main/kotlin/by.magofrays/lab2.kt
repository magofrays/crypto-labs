package by.magofrays

fun main() {
//    val SBlock1 = byteArrayOf(12, 14, 1, 0, 11, 5, 9, 7, 4, 10, 6, 8, 3, 13, 2, 15)
//    printTable(findDDT(SBlock1, SBlock1.size))

    val magmaBlockS1 = byteArrayOf(12, 4, 6, 2, 10, 5, 11, 9, 14, 8, 13, 7, 0, 3, 15, 1)
    val magmaBlockS2 = byteArrayOf(6, 8, 2, 3, 9, 10, 5, 12, 1, 14, 4, 7, 11, 13, 0, 15)
    printTable(findDDT(magmaBlockS2, magmaBlockS2.size))
}

fun printTable(table: DDTable) {
    for (item in table.table) {
        println(item.map { it.count })
    }
    table.sortedDD.take(10).forEach { println(it) }
}

fun findDDT(SBlock: ByteArray, diffElements: Int): DDTable {
    val table = MutableList(diffElements) { dx ->
        MutableList(diffElements) { dy ->
            DD(deltaX = dx, deltaY = dy, count = 0, probability = 0.0)
        }
    }
    for (dx in 0 until diffElements) {
        for (x in 0 until diffElements) {
            val x2 = x xor dx
            val dy = (SBlock[x].toInt() xor SBlock[x2].toInt()) and 0xF
            table[dx][dy].count++
        }
    }
    val total = diffElements.toDouble()
    val sortedDD = mutableListOf<DD>()
    for (row in table) {
        for (dd in row) {
            dd.probability = dd.count / total
            if (dd.deltaX != 0) sortedDD.add(dd)
        }
    }
    sortedDD.sortByDescending { it.probability }
    return DDTable(table = table, sortedDD = sortedDD)
}


data class DDTable(
    val table: List<List<DD>>,
    val sortedDD: List<DD>
)

data class DD(
    val deltaX: Int,
    val deltaY: Int,
    var count: Int,
    var probability: Double
)