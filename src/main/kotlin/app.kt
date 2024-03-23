import com.github.shiguruikai.combinatoricskt.powerset
import kotlin.random.Random

private val i1: Int
    get() {
        var step = 0
        return step
    }

fun main() {
//    loadTextBoard(easyBoard1)
//    loadTextBoard(expertBoard1)
//    loadTextBoard(extremeBoard1)
    loadTextBoard(extremeBoard2)

    printBoard()


    for (i in 0 until 9) {
        for (j in 0 until 9) {
            if (values[i][j] > 0) makeImpossible(j, i, values[i][j])
        }
    }

    var result = StepState.Excluded

    while (result != StepState.NoResult) {
        println("\nstep ${++step}")
        result = solveNext()

        when (result) {
            StepState.Found -> {
                printBoard()
//                println()
//                printCandidates()

                validate()
            }

            StepState.Excluded -> {
                printCandidates()
                validate()
            }

            StepState.GoAhead, StepState.NoResult -> {
                println("[no changes]")
//                printCandidates()

                if (values.all { it.all { v -> v > 0 } }) {
                    println("Разгадано!")
                    return
                }

                var x = 0
                var y = 0
                var cypher: Cypher = 0

                while (cypher == 0.toByte()) {
                    x = Random.nextInt(9)
                    y = Random.nextInt(9)
                    cypher = possible[y][x].let { if (it.isEmpty()) 0 else it.random() }
                }

                saveState(cypher, x to y)
                println("Пробуем рандом. $cypher на позицию ${x + 1}, ${y + 1}")

                write(x, y, cypher)
                printBoard()

                validate()

                result = StepState.GoAhead
            }
        }
    }
}

fun validate() {
    for (i in 0..8) {
        for (j in 0..8) {
            if (values[i][j] < 1 && possible[i][j].isEmpty()) {
                println("Откат из-за пустой клетки ${j + 1}, ${i + 1}")
                val state = popState()
                println("Удаляем вариант ${state.cypher} из ${state.p.first + 1}, ${state.p.second + 1}")

                possible[state.p.second][state.p.first].remove(state.cypher)

                printCandidates()
                return
            }
        }
    }
}

typealias Cypher = Byte

var step = 0

var highlightX: Int = -1
var highlightY: Int = -1
var candidatesHighlights = mutableListOf<Pair<Int, Int>>()

enum class StepState { Found, Excluded, NoResult, GoAhead }
typealias IdxConverter = (Int) -> Pair<Int, Int>
typealias ExclusionProvider = (Int) -> List<Pair<Int, Int>>

fun solveNext(): StepState {
    highlightX = -1
    highlightY = -1
    candidatesHighlights.clear()

    for (i in 0 until 9) {
        for (j in 0 until 9) {
            if (possible[i][j].count() == 1) {
                write(j, i, possible[i][j].first())
                return StepState.Found
            }
        }
    }

    for (i in 0..<9) {
        val possibleSet: List<MutableList<Cypher>> = possible[i].toList()
        val converter: IdxConverter = { idx -> (idx to i) }

        findHiddenGroup(possibleSet)?.also { (cyphers, indices) ->
            println("Скрытая группа. Цифры ${cyphers}. Строка ${i + 1}. Индексы ${indices.map { it + 1 }}")

            handleHiddenGroup(possibleSet, converter, cyphers, indices).also { if (it != StepState.GoAhead) return it }

            candidatesHighlights.clear()
        }

        val exclusionProvider: ExclusionProvider = { third ->
            IntRange(i / 3 * 3, i / 3 * 3 + 2).minus(i)
                .flatMap { y -> IntRange(third * 3, third * 3 + 2).map { x -> x to y } }
        }

        if (findAndHandleThirds(possibleSet, converter, i, "строке", "одном квадрате", exclusionProvider)) {
            return StepState.Excluded
        }
    }

    for (j in 0..<9) {
        val possibleSet: List<MutableList<Cypher>> = possible.map { it[j] }
        val converter: IdxConverter = { idx -> (j to idx) }

        findHiddenGroup(possibleSet)?.also { (cyphers, indices) ->
            println("Скрытая группа. Цифры ${cyphers}. Столбец ${j + 1}. Индексы ${indices.map { it + 1 }}")

            handleHiddenGroup(possibleSet, converter, cyphers, indices).also { if (it != StepState.GoAhead) return it }

            candidatesHighlights.clear()
        }

        val exclusionProvider: ExclusionProvider = { third ->
            IntRange(j / 3 * 3, j / 3 * 3 + 2).minus(j)
                .flatMap { x -> IntRange(third * 3, third * 3 + 2).map { y -> x to y } }
        }

        if (findAndHandleThirds(possibleSet, converter, j, "столбце", "одном квадрате", exclusionProvider)) {
            return StepState.Excluded
        }
    }

    for (xd in intArrayOf(0, 3, 6)) {
        for (yd in intArrayOf(0, 3, 6)) {
            val possibleSet: List<MutableList<Cypher>> =
                (0..2).flatMap { y -> (0..2).map { y to it } }.map { (y, x) -> possible[yd + y][xd + x] }

            val converter: IdxConverter = { idx -> (xd + idx % 3) to (yd + idx / 3) }

            findHiddenGroup(possibleSet)?.also { (cyphers, indices) ->
                println("Скрытая группа. Цифры ${cyphers}. Квадрат ${xd / 3 + 1 + yd}. Индексы ${indices.map { it + 1 }}")

                handleHiddenGroup(
                    possibleSet,
                    converter,
                    cyphers,
                    indices
                ).also { if (it != StepState.GoAhead) return it }

                candidatesHighlights.clear()
            }

            val exclusionProvider: ExclusionProvider = { third ->
                val xs = when (xd) {
                    0 -> 3..8
                    6 -> 0..5
                    else -> (0..2).plus(6..8)
                }
                xs.map { x -> x to (yd + third) }
            }

            if (findAndHandleThirds(
                    possibleSet, converter, xd / 3 + 1 + yd, "квадрате", "одной строке", exclusionProvider
                )
            ) {
                return StepState.Excluded
            }

            val possibleSetTransposed: List<MutableList<Cypher>> =
                (0..2).flatMap { x -> (0..2).map { x to it } }.map { (x, y) -> possible[yd + y][xd + x] }
            val exclusionProviderTransposed: ExclusionProvider = { third ->
                val ys = when (yd) {
                    0 -> 3..8
                    6 -> 0..5
                    else -> (0..2).plus(6..8)
                }
                ys.map { y -> (xd + third) to y }
            }

            if (findAndHandleThirds(
                    possibleSetTransposed,
                    converter,
                    xd / 3 + 1 + yd,
                    "квадрате",
                    "одном столбце",
                    exclusionProviderTransposed
                )
            ) {
                return StepState.Excluded
            }
        }
    }

    if (findRectangles()) return StepState.Excluded

    return StepState.NoResult
}

fun findRectangles(): Boolean {
    for (cypher in CYPHERS) {
        val cells = (0..8).flatMap { x -> (0..8).map { x to it } }.filter { (x, y) -> possible[y][x].contains(cypher) }

        val xCoordGetter = { p: Pair<Int, Int> -> p.first }
        val yCoordGetter = { p: Pair<Int, Int> -> p.second }

        if (findRectangleDirectional(cypher, cells, xCoordGetter, yCoordGetter, "x" to "y", 2)) return true
        if (findRectangleDirectional(cypher, cells, yCoordGetter, xCoordGetter, "y" to "x", 2)) return true

        if (findRectangleDirectional(cypher, cells, xCoordGetter, yCoordGetter, "x" to "y", 3)) return true
        if (findRectangleDirectional(cypher, cells, yCoordGetter, xCoordGetter, "y" to "x", 3)) return true
    }

    return false
}

private fun findRectangleDirectional(
    cypher: Cypher,
    cells: List<Pair<Int, Int>>,
    lineCoordGetter: (Pair<Int, Int>) -> Int,
    posCoordGetter: (Pair<Int, Int>) -> Int,
    coords: Pair<String, String>,
    count: Int
): Boolean {
    val lines: List<List<Pair<Int, Int>>> =
        (0..8).map { idx -> cells.filter { lineCoordGetter(it) == idx } }.filter { it.count() == 2 }

    if (lines.count() == count) {
        val positions = lines.flatMap { it.map(posCoordGetter) }.distinct()

        if (positions.count() == count && lines.sumOf { it.count() } == count * 2) {
            val lineNums = lines.map { it.first() }.map(lineCoordGetter)

            println(
                "Найден прямоугольник для $cypher: ${coords.first}=${lineNums.map { it + 1 }}, ${coords.second}=${positions.map { it + 1 }}"
            )

            val removals: List<MutableList<Cypher>> =
                (0..8).minus(lineNums).flatMap { line -> positions.map { pos -> possible[pos][line] } }

            if (removals.map { it.remove(cypher) }.any { it }) {
                return true
            }
        }
    }
    return false
}
//fun findRectangles(): Boolean {
//    val combinations = CYPHERS.combinations(2)
//
//    for (comb in combinations) {
//        val cells = (0..8).flatMap { x -> (0..8).map { x to it } }
//            .filter { (x, y) -> possible[y][x].contains(comb.first()) && possible[y][x].contains(comb.last()) }
//
//        val rows: List<List<Pair<Int, Int>>> =
//            (0..8).map { row -> cells.filter { (x, y) -> y == row } }.filter { it.count() == 2 }
//
//        if (rows.count() == 2) {
//            val columns = rows.flatMap { it.map { (x, y) -> x } }.toSet()
//            if (columns.count() == 2) {
//                println(
//                    "Найден прямоугольник для $comb: x=${columns.map { it + 1 }}, y=${
//                        rows.flatMap { it.map { it.second + 1 } }.distinct()
//                    }"
//                )
//                return true
//            }
//        }
//    }
//
//    return false
//}

private fun findAndHandleThirds(
    possibleSet: List<MutableList<Cypher>>,
    converter: IdxConverter,
    i: Int,
    direction: String,
    placement: String,
    exclusionProvider: ExclusionProvider
): Boolean {
    var result = false

    val cellsByCypher: Map<Cypher, List<Int>> =
        possibleSet.withIndex().flatMap { (idx, cypherList) -> cypherList.map { it to idx } }
            .groupBy({ (cypher, _) -> cypher }, { (_, idx) -> idx })

    val cyphersInsideThirds = cellsByCypher.filter { (_, indices) -> indices.map { it / 3 }.distinct().count() == 1 }
    if (cyphersInsideThirds.isNotEmpty()) {
        for ((cypher, indices) in cyphersInsideThirds) {
            val removals = exclusionProvider(indices.first() / 3).map { (x, y) -> possible[y][x].remove(cypher) }

            if (removals.any { it }) {
                println("Цифра ${cypher} в $direction ${i + 1} может быть только в $placement (позиции ${indices.map { it + 1 }}).")
                indices.forEach { idx -> candidatesHighlights.add(converter(idx)) }
                result = true
            }
        }
    }

    return result
}

private fun handleHiddenGroup(
    possibleSet: List<MutableList<Cypher>>, converter: IdxConverter, cyphers: Set<Cypher>, indices: Set<Int>
): StepState {
    if (cyphers.count() == 1) {
        val (x, y) = converter(indices.first())
        write(x, y, cyphers.first())
        return StepState.Found
    } else {
        var result = StepState.GoAhead
        for (idx in 0..<9) {
            if (idx !in indices) cyphers.forEach { if (possibleSet[idx].remove(it)) result = StepState.Excluded }
        }
        for (cypher in CYPHERS.minus(cyphers).toList()) {
            indices.forEach { idx -> if (possibleSet[idx].remove(cypher)) result = StepState.Excluded }
        }

        indices.forEach { idx -> candidatesHighlights.add(converter(idx)) }

        return result
    }
}

fun findHiddenGroup(possibleSet: List<List<Cypher>>): Pair<Set<Cypher>, Set<Int>>? {
    val freeCellCount = possibleSet.count { it.isNotEmpty() }
    val cyphers: Set<Cypher> = possibleSet.flatten().toSet()

    val cellsByCypher: Map<Cypher, List<Int>> =
        possibleSet.withIndex().flatMap { (idx, cypherList) -> cypherList.map { it to idx } }
            .groupBy({ (cypher, _) -> cypher }, { (_, idx) -> idx })

    val cypherSets: Sequence<List<Cypher>> = cyphers.powerset().filter { it.count() in 1..<freeCellCount }

    for (cypherSet in cypherSets) {
        val cells: Set<Int> = cypherSet.flatMap { cypher -> cellsByCypher.getValue(cypher) }.toSet()
        if (cells.count() == cypherSet.count()) {
            return cypherSet.toSet() to cells
        }
    }

    return null
}

fun countCyphers(possibleSet: List<List<Cypher>>): Map<Cypher, Int> {
    return possibleSet.flatten().groupingBy { it }.eachCount()
}

fun write(x: Int, y: Int, value: Cypher) {
    values[y][x] = value
    makeImpossible(x, y, value)

    highlightX = x
    highlightY = y
}

fun removeCandidate(idxConverter: IdxConverter, idx: Int, value: Cypher) {
    val (x, y) = idxConverter(idx)
    possible[y][x].remove(value)
}

fun makeImpossible(x: Int, y: Int, value: Cypher) {
    possible[y][x].clear()

    // row
    for (j in 0 until 9) {
        if (j != x) {
            possible[y][j].remove(value)
        }
    }
    // column
    for (i in 0 until 9) {
        if (i != y) {
            possible[i][x].remove(value)
        }
    }
    // square
    val rect: Rect = getRect(x, y)
    for (i in rect.y1..rect.y2) {
        for (j in rect.x1..rect.x2) {
            if (i != y && j != x) possible[i][j].remove(value)
        }
    }
}

fun getRect(x: Int, y: Int): Rect {
    val xNum = x / 3
    val yNum = y / 3
    return Rect(xNum * 3, yNum * 3, xNum * 3 + 2, yNum * 3 + 2)
}

data class Rect(val x1: Int, val y1: Int, val x2: Int, val y2: Int)

val easyBoard1 = """
    25-------
    369754182
    -4813296-
    69----7--
    187-2945-
    ----67819
    4--29863-
    83-671--4
    91-5---78
""".trimIndent()

val expertBoard1 = """
    4-------8
    -5-7-23--
    --619--2-
    16-35-9--
    ------7--
    --8------
    --------5
    -------4-
    -3-67----
""".trimIndent()

val extremeBoard1 = """
    ---9--2--
    -85------
    ----6----
    ---7-8---
    9-----4--
    ---5-----
    2---1--7-
    ------6-3
    -------58
""".trimIndent()

val extremeBoard2 = """
    ----3---8
    4-----5--
    -9-7-----
    ----586--
    -76------
    -----9---
    ---2---9-
    8-3------
    5--------
""".trimIndent()

private fun loadTextBoard(board: String) {
    var j = 0
    for (i in 0 until board.count()) {
        if (!board[i].isWhitespace()) {
            values[j / 9][j % 9] = if (board[i] in '1'..'9') board[i].digitToInt().toByte() else 0
            j++
        }
    }
}

fun printBoard() {
    for (i in 0 until 9) {
        for (j in 0 until 9) {
            if (values[i][j] > 0) {
                if (j == highlightX && i == highlightY) print("\u001B[42m ${values[i][j]} \u001B[0m")
                else print(" ${values[i][j]} ")
            } else print("   ")

            if (j < 8 && j % 3 == 2) print("|")
        }

        println()

        if (i < 8 && i % 3 == 2) println("-".repeat(29))
//        else println()
    }
}

fun printCandidates() {
    for (i in 0 until 9) {
        for (y in 0..<3) {
            for (j in 0 until 9) {

                if (values[i][j] > 0) {
                    print("\u001B[40m")
                    if (y == 1) print("    ${values[i][j]}    ")
                    else print("         ")
                    print("\u001B[0m")
                } else {
                    if (candidatesHighlights.contains(j to i)) print("\u001B[42m")

                    for (x in 0..<3) {
//                        if (x == 1)
                        print(" ")

                        if (possible[i][j].contains((y * 3 + x + 1).toByte())) print(y * 3 + x + 1)
                        else print(" ")

//                        if (x == 1)
                        print(" ")
                    }

                    if (candidatesHighlights.contains(j to i)) print("\u001B[0m")
                }

                if (j < 8 && j % 3 == 2) print("|")
                else print(".")
            }

            println()
        }

        if (i < 8 && i % 3 == 2) println("-".repeat(90))
        else println(". ".repeat(45))
    }
}

val CYPHERS: List<Cypher> = (1..9).map { it.toByte() }

var values = Array(9) { ByteArray(9) }
var possible: Array<Array<MutableList<Cypher>>> =
    Array(9) { Array(9) { byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9).toMutableList() } }

data class State(
    val values: Array<ByteArray>,
    val possible: Array<Array<List<Cypher>>>,
    val cypher: Cypher,
    val p: Pair<Int, Int>
)

val stack = mutableListOf<State>()
fun saveState(cypher: Cypher, p: Pair<Int, Int>) {
    val valuesCopy = Array(9) { values[it].copyOf() }
    val possibleCopy = Array(9) { i -> Array(9) { j -> possible[i][j].toList() } }
    stack.add(State(valuesCopy, possibleCopy, cypher, p))
}

fun popState(): State {
    val state = stack.last()
    stack.removeLast()
    values = state.values
    possible = Array(9) { i -> Array(9) { j -> state.possible[i][j].toMutableList() } }
    return state
}
