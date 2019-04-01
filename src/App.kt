import java.io.*
import java.nio.file.Files
import java.util.*
import kotlin.collections.HashMap

fun main(args: Array<String>) {
    compress()
    decompress()
}

private fun compress() {
    val inputBytes = File("book.txt").let { Files.readAllBytes(it.toPath()) }
    val counts = ArrayList<Pair<Byte, Int>>()
    inputBytes.forEach { byte ->
        (counts.find { it.first == byte }
                ?.let {
                    counts.remove(it)
                    Pair(byte, it.second + 1)
                } ?: Pair(byte, 1))
                .let { counts.add(it) }
    }
    counts.sortWith(kotlin.Comparator { o1, o2 -> o1.second.compareTo(o2.second) })
    val tree = counts.map { Pair(Node(it.first), it.second) }.toMutableList()
    while (tree.size != 1) {
        val first = tree.removeAt(0)
        val second = tree.removeAt(0)
        tree.add(Pair(Node(null, first.first, second.first), first.second + second.second))
        tree.sortWith(kotlin.Comparator { o1, o2 -> o1.second.compareTo(o2.second) })
    }
    val root = tree[0].first
    val table = HashMap<Byte, String>()
    fun writeToTable(node: Node, path: String) {
        node.value?.let { table.put(it, path) } ?: run {
            node.left?.let { writeToTable(it, "${path}0") }
            node.right?.let { writeToTable(it, "${path}1") }
        }
    }
    writeToTable(root, "")
    val outputBytes = ArrayList<Byte>()
    var buffer = ""
    inputBytes.forEach { byte ->
        buffer += table[byte]!!
        if (buffer.length >= 8) {
            val bitString = buffer.substring(0..7)
            outputBytes.add(bitStringToByte(bitString))
            buffer = buffer.substring(8)
        }
    }
    outputBytes.add(bitStringToByte(buffer))
    val lengthOfFileInBites = (outputBytes.size.toLong() - 1) * 8 + buffer.length
    val lengthOfTableInEntries = table.size
    val output = DataOutputStream(FileOutputStream("compressed"))
    output.writeLong(lengthOfFileInBites)
    output.writeByte(lengthOfTableInEntries)
    table.forEach { byte, string ->
        output.writeByte(byte.toPositiveInt())
        output.writeByte(string.length)
        output.writeInt(bitStringToInt(string))
    }
    output.write(outputBytes.toByteArray())
    output.close()
}

private fun decompress() {
    val input = DataInputStream(FileInputStream("compressed"))
    val lengthOfFileInBites = input.readLong()
    val lengthOfTableInEntries = input.readByte().toPositiveInt()
    val root = Node(null)
    for (i in 0 until lengthOfTableInEntries) {
        val byte = input.readByte()
        val codeLength = input.readByte().toPositiveInt()
        val code = input.readInt()
        val bitString = intToBitString(code, codeLength)
        var node = root
        bitString.forEach {
            if (it == '0') (node.left ?: Node(null).also { node.left = it }).let { node = it }
            else (node.right ?: Node(null).also { node.right = it }).let { node = it }
        }
        node.value = byte
    }
    val lengthOfFileInBytes = lengthOfFileInBites / 8 + if (lengthOfFileInBites % 8 != 0L) 1 else 0
    var node = root
    val output = DataOutputStream(FileOutputStream("decompressed.txt"))
    for (i in 0 until lengthOfFileInBytes) {
        val bitString = byteToBitString(
                input.readUnsignedByte(),
                if (i == lengthOfFileInBytes - 1) (lengthOfFileInBites - i * 8).toInt() else 8
        )
        bitString.forEach {
            node = if (it == '0') node.left!! else node.right!!
            node.value?.let {
                output.writeByte(it.toPositiveInt())
                node = root
            }
        }
    }
    input.close()
    output.close()
}

private fun bitStringToByte(byteString: String): Byte {
    var result = 0
    byteString.forEachIndexed { index, char -> result += (if (char == '1') 1 else 0) shl 7 - index }
    return result.toByte()
}

private fun bitStringToInt(byteString: String): Int {
    var result = 0
    byteString.forEachIndexed { index, char -> result += (if (char == '1') 1 else 0) shl 31 - index }
    return result
}

private fun intToBitString(int: Int, length: Int): String {
    var result = ""
    for (i in 0 until length)
        result += if (int ushr 31 - i and 1 == 1) '1' else '0'
    return result
}

private fun byteToBitString(byte: Int, length: Int): String {
    var result = ""
    for (i in 0 until length)
        result += if (byte ushr 7 - i and 1 == 1) '1' else '0'
    return result
}

fun Byte.toPositiveInt() = toInt() and 0xFF
