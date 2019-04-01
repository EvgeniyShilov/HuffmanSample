class Node(
        var value: Byte?,
        var left: Node? = null,
        var right: Node? = null
) {
    override fun toString(): String {
        return if (value == null) "($left, $right)" else value.toString()
    }
}
