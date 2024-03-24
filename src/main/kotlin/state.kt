private data class State(
    val values: Array<ByteArray>,
    val candidates: Array<Array<MutableList<Cypher>>>,
    val cypher: Cypher,
    val coords: Coords
)

private var currentState = State(
    Array(9) { ByteArray(9) },
    Array(9) { Array(9) { byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9).toMutableList() } },
    0, 0 to 0
)

fun values(): Array<ByteArray> = currentState.values
fun candidates(): Array<Array<MutableList<Cypher>>> = currentState.candidates

private val stack = mutableListOf<State>()

fun saveState(cypher: Cypher, p: Pair<Int, Int>) {
    val valuesCopy = Array(9) { values()[it].copyOf() }
    val possibleCopy = Array(9) { i -> Array(9) { j -> candidates()[i][j].toMutableList() } }
    stack.add(State(valuesCopy, possibleCopy, cypher, p))
}

fun popState(): Pair<Cypher, Coords> {
    currentState = stack.last()
    stack.removeLast()
    return currentState.cypher to currentState.coords
}