import com.github.shiguruikai.combinatoricskt.powerset
import kotlin.random.Random


enum class StepState { Found, Excluded, NoResult, GoAhead }
typealias Coords = Pair<Int, Int>
typealias IdxConverter = (Int) -> Coords
typealias ExclusionProvider = (Int) -> List<Coords>
typealias Cypher = Byte

val CYPHERS: List<Cypher> = (1..9).map { it.toByte() }
val INDICES: IntRange = 0..8

fun main() {
    loadPuzzle(Puzzles.extremeBoard2)
    Screen.printBoard()

    var result = StepState.Excluded
    var step = 0

    while (result != StepState.NoResult) {
        println("\nstep ${++step}")
        result = solveNext()

        when (result) {
            StepState.Found -> {
                Screen.printBoard()
                validate()
            }

            StepState.Excluded -> {
                Screen.printCandidates()
                validate()
            }

            StepState.GoAhead, StepState.NoResult -> {
                println("[no changes]")

                if (values().all { it.all { v -> v > 0 } }) {
                    println("Разгадано!")
                    return
                }

                var x = 0
                var y = 0
                var cypher: Cypher = 0

                while (cypher == 0.toByte()) {
                    x = Random.nextInt(9)
                    y = Random.nextInt(9)
                    cypher = candidates()[y][x].let { if (it.isEmpty()) 0 else it.random() }
                }

                saveState(cypher, x to y)
                println("Пробуем рандом. $cypher на позицию ${x + 1}, ${y + 1}")

                write(x, y, cypher)
                Screen.printBoard()

                validate()

                result = StepState.GoAhead
            }
        }
    }
}

fun validate() {
    for (i in INDICES) {
        for (j in INDICES) {
            if (values()[i][j] < 1 && candidates()[i][j].isEmpty()) {
                println("Откат из-за пустой клетки ${j + 1}, ${i + 1}")
                val (cypher, coords) = popState()
                println("Удаляем вариант ${cypher} из ${coords.first + 1}, ${coords.second + 1}")

                candidates()[coords.second][coords.first].remove(cypher)

                Screen.printCandidates()
                return
            }
        }
    }
}


fun solveNext(): StepState {
    Screen.clearValueHighlight()
    Screen.clearCandidateHighlights()

    for (i in INDICES) {
        for (j in INDICES) {
            if (candidates()[i][j].count() == 1) {
                write(j, i, candidates()[i][j].first())
                return StepState.Found
            }
        }
    }

    for (i in INDICES) {
        val candidatesForCells: List<MutableList<Cypher>> = candidates()[i].toList()
        val converter: IdxConverter = { idx -> (idx to i) }

        findHiddenGroup(candidatesForCells)?.also { (cyphers, indices) ->
            println("Скрытая группа. Цифры ${cyphers}. Строка ${i + 1}. Индексы ${indices.map { it + 1 }}")

            handleHiddenGroup(
                candidatesForCells,
                converter,
                cyphers,
                indices
            ).also { if (it != StepState.GoAhead) return it }

            Screen.clearCandidateHighlights()
        }

        val exclusionProvider: ExclusionProvider = { third ->
            IntRange(i / 3 * 3, i / 3 * 3 + 2).minus(i)
                .flatMap { y -> IntRange(third * 3, third * 3 + 2).map { x -> x to y } }
        }

        if (findAndHandleThirds(candidatesForCells, converter, i, "строке", "одном квадрате", exclusionProvider)) {
            return StepState.Excluded
        }
    }

    for (j in INDICES) {
        val candidatesForCells: List<MutableList<Cypher>> = candidates().map { it[j] }
        val converter: IdxConverter = { idx -> (j to idx) }

        findHiddenGroup(candidatesForCells)?.also { (cyphers, indices) ->
            println("Скрытая группа. Цифры ${cyphers}. Столбец ${j + 1}. Индексы ${indices.map { it + 1 }}")

            handleHiddenGroup(
                candidatesForCells,
                converter,
                cyphers,
                indices
            ).also { if (it != StepState.GoAhead) return it }

            Screen.clearCandidateHighlights()
        }

        val exclusionProvider: ExclusionProvider = { third ->
            IntRange(j / 3 * 3, j / 3 * 3 + 2).minus(j)
                .flatMap { x -> IntRange(third * 3, third * 3 + 2).map { y -> x to y } }
        }

        if (findAndHandleThirds(candidatesForCells, converter, j, "столбце", "одном квадрате", exclusionProvider)) {
            return StepState.Excluded
        }
    }

    for (xd in INDICES.step(3)) {
        for (yd in INDICES.step(3)) {
            val candidatesForCells: List<MutableList<Cypher>> =
                (0..2).flatMap { y -> (0..2).map { y to it } }.map { (y, x) -> candidates()[yd + y][xd + x] }

            val converter: IdxConverter = { idx -> (xd + idx % 3) to (yd + idx / 3) }

            findHiddenGroup(candidatesForCells)?.also { (cyphers, indices) ->
                println("Скрытая группа. Цифры ${cyphers}. Квадрат ${xd / 3 + 1 + yd}. Индексы ${indices.map { it + 1 }}")

                handleHiddenGroup(
                    candidatesForCells,
                    converter,
                    cyphers,
                    indices
                ).also { if (it != StepState.GoAhead) return it }

                Screen.clearCandidateHighlights()
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
                    candidatesForCells, converter, xd / 3 + 1 + yd, "квадрате", "одной строке", exclusionProvider
                )
            ) {
                return StepState.Excluded
            }

            val possibleSetTransposed: List<MutableList<Cypher>> =
                (0..2).flatMap { x -> (0..2).map { x to it } }.map { (x, y) -> candidates()[yd + y][xd + x] }
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
        val cells =
            (INDICES).flatMap { x -> (INDICES).map { x to it } }
                .filter { (x, y) -> candidates()[y][x].contains(cypher) }

        val xCoordGetter = { p: Coords -> p.first }
        val yCoordGetter = { p: Coords -> p.second }

        if (findRectangleDirectional(cypher, cells, xCoordGetter, yCoordGetter, "x" to "y", 2)) return true
        if (findRectangleDirectional(cypher, cells, yCoordGetter, xCoordGetter, "y" to "x", 2)) return true

        if (findRectangleDirectional(cypher, cells, xCoordGetter, yCoordGetter, "x" to "y", 3)) return true
        if (findRectangleDirectional(cypher, cells, yCoordGetter, xCoordGetter, "y" to "x", 3)) return true
    }

    return false
}

private fun findRectangleDirectional(
    cypher: Cypher,
    cellsWithCandidate: List<Coords>,
    lineCoordGetter: (Coords) -> Int,
    posCoordGetter: (Coords) -> Int,
    coords: Pair<String, String>,
    count: Int
): Boolean {
    val lines: List<List<Coords>> =
        INDICES.map { idx -> cellsWithCandidate.filter { lineCoordGetter(it) == idx } }.filter { it.count() == 2 }

    if (lines.count() == count) {
        val positions = lines.flatMap { it.map(posCoordGetter) }.distinct()

        if (positions.count() == count && lines.sumOf { it.count() } == count * 2) {
            val lineNums = lines.map { it.first() }.map(lineCoordGetter)

            println(
                "Найден прямоугольник для $cypher: ${coords.first}=${lineNums.map { it + 1 }}, ${coords.second}=${positions.map { it + 1 }}"
            )

            val removals: List<MutableList<Cypher>> =
                INDICES.minus(lineNums).flatMap { line -> positions.map { pos -> candidates()[pos][line] } }

            if (removals.map { it.remove(cypher) }.any { it }) {
                return true
            }
        }
    }
    return false
}

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
            val removals = exclusionProvider(indices.first() / 3).map { (x, y) -> candidates()[y][x].remove(cypher) }

            if (removals.any { it }) {
                println("Цифра ${cypher} в $direction ${i + 1} может быть только в $placement (позиции ${indices.map { it + 1 }}).")
                indices.forEach { idx -> Screen.addCandidateHighlight(converter(idx)) }
                result = true
            }
        }
    }

    return result
}

private fun handleHiddenGroup(
    candidatesForCells: List<MutableList<Cypher>>, converter: IdxConverter, cyphers: Set<Cypher>, groupIndices: Set<Int>
): StepState {
    if (cyphers.count() == 1) {
        val (x, y) = converter(groupIndices.first())
        write(x, y, cyphers.first())
        return StepState.Found
    } else {
        var result = StepState.GoAhead
        for (idx in INDICES) {
            if (idx !in groupIndices) cyphers.forEach {
                if (candidatesForCells[idx].remove(it)) result = StepState.Excluded
            }
        }
        for (cypher in CYPHERS.minus(cyphers).toList()) {
            groupIndices.forEach { idx -> if (candidatesForCells[idx].remove(cypher)) result = StepState.Excluded }
        }

        groupIndices.forEach { idx -> Screen.addCandidateHighlight(converter(idx)) }

        return result
    }
}

fun findHiddenGroup(candidatesForCells: List<List<Cypher>>): Pair<Set<Cypher>, Set<Int>>? {
    val freeCellCount = candidatesForCells.count { it.isNotEmpty() }
    val cyphers: Set<Cypher> = candidatesForCells.flatten().toSet()

    val indicesByCypher: Map<Cypher, List<Int>> =
        candidatesForCells.withIndex().flatMap { (idx, cypherList) -> cypherList.map { it to idx } }
            .groupBy({ (cypher, _) -> cypher }, { (_, idx) -> idx })

    val cypherSets: Sequence<List<Cypher>> = cyphers.powerset().filter { it.count() in 1..<freeCellCount }

    for (cypherSet in cypherSets) {
        val cells: Set<Int> = cypherSet.flatMap { cypher -> indicesByCypher.getValue(cypher) }.toSet()
        if (cells.count() == cypherSet.count()) {
            return cypherSet.toSet() to cells
        }
    }

    return null
}

fun write(x: Int, y: Int, value: Cypher) {
    values()[y][x] = value
    removeValueFromCandidates(x, y, value)

    Screen.highlightValue(x to y)
}

fun removeValueFromCandidates(x: Int, y: Int, value: Cypher) {
    candidates()[y][x].clear()

    // row
    for (j in INDICES) {
        if (j != x) {
            candidates()[y][j].remove(value)
        }
    }
    // column
    for (i in INDICES) {
        if (i != y) {
            candidates()[i][x].remove(value)
        }
    }

    // square
    data class Rect(val x1: Int, val y1: Int, val x2: Int, val y2: Int)
    fun getRect(x: Int, y: Int): Rect {
        val xNum = x / 3
        val yNum = y / 3
        return Rect(xNum * 3, yNum * 3, xNum * 3 + 2, yNum * 3 + 2)
    }

    val rect: Rect = getRect(x, y)
    for (i in rect.y1..rect.y2) {
        for (j in rect.x1..rect.x2) {
            if (i != y && j != x) candidates()[i][j].remove(value)
        }
    }
}

private fun loadPuzzle(board: String) {
    var i = 0
    for (char in board) {
        if (!char.isWhitespace()) {
            values()[i / 9][i % 9] = if (char in '1'..'9') char.digitToInt().toByte() else 0
            i++
        }
    }

    applyToBoard { (x, y) ->
            if (values()[y][x] > 0) removeValueFromCandidates(x, y, values()[y][x])
    }
}

fun applyToBoard(func: (Coords) -> Unit) {
    for (i in INDICES) {
        for (j in INDICES) {
            func(j to i)
        }
    }
}
