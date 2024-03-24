import java.util.*

object Screen {
    private var valueHighlight: Optional<Coords> = Optional.empty()
    private val candidatesHighlights = mutableListOf<Coords>()

    fun highlightValue(c: Coords) {
        valueHighlight = Optional.of(c)
    }

    fun clearValueHighlight() {
        valueHighlight = Optional.empty()
    }

    fun addCandidateHighlight(c: Coords) {
        candidatesHighlights.add(c)
    }

    fun clearCandidateHighlights() {
        candidatesHighlights.clear()
    }

    fun printBoard() {
        for (i in INDICES) {
            for (j in INDICES) {
                if (values()[i][j] > 0) {
                    if (valueHighlight.map { it == (j to i) }.orElse(false)) {
                        print("\u001B[42m ${values()[i][j]} \u001B[0m")
                    } else {
                        print(" ${values()[i][j]} ")
                    }
                } else print("   ")

                if (j < 8 && j % 3 == 2) print("|")
            }

            println()

            if (i < 8 && i % 3 == 2) println("-".repeat(29))
        }
    }

    fun printCandidates() {
        for (i in 0 until 9) {
            for (y in 0..<3) {
                for (j in 0 until 9) {

                    if (values()[i][j] > 0) {
                        print("\u001B[40m")
                        if (y == 1) print("    ${values()[i][j]}    ")
                        else print("         ")
                        print("\u001B[0m")
                    } else {
                        if (candidatesHighlights.contains(j to i)) print("\u001B[42m")

                        for (x in 0..<3) {
                            print(" ")

                            if (candidates()[i][j].contains((y * 3 + x + 1).toByte())) print(y * 3 + x + 1)
                            else print(" ")

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
}