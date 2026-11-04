package by.magofrays.lb3

val sBox = arrayOf(
    intArrayOf(252, 238, 221, 17),
    intArrayOf(223, 119, 240, 219),
    intArrayOf(249, 24, 101, 90),
    intArrayOf(5, 132, 2, 174)
)

fun substitute(block: IntArray): IntArray {
    return IntArray(block.size) { idx ->
        val b = block[idx] and 0xFF
        val i = (b shr 6) and 0x03
        val j = b and 0x03
        sBox[i][j]
    }
}

fun main() {
    val inputSingle = intArrayOf(191)
    val outputSingle = substitute(inputSingle)

    println("--- Control Example from Task ---")
    println("Input byte: ${inputSingle[0]} (binary: ${Integer.toBinaryString(inputSingle[0])})")
    println("Output byte: ${outputSingle[0]}")
    println("Expected: 240")
    println()

    // Demonstration of processing a block of fixed length n
    println("--- Block Data Processing ---")
    val block = intArrayOf(191, 50, 55, 49, 56, 0, 255, 128)
    val transformedBlock = substitute(block)

    println("Original block:    ${block.joinToString(", ")}")
    println("Transformed block: ${transformedBlock.joinToString(", ")}")
}