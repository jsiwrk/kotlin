// !LANGUAGE: +CoerceNonExhaustiveWhenToUnit

fun one() = 1
fun two() = 2

fun test1(): Int {
    while (true) {
        when (one()) {
            1 -> {
                when(two()) {
                    2 -> return 2
                }
            }
            else -> return 3
        }
    }
}

fun test2(): Int {
    while (true) {
        when (one()) {
            1 ->
                when (two()) {
                    2 -> return 2
                }
            else -> return 3
        }
    }
}
