// !LANGUAGE: +CoerceNonExhaustiveWhenToUnit

fun println() {}

fun test1(x: Int) {
    run {
        when (x) {
            1 -> <!UNUSED_EXPRESSION!>true<!>
        }
    }
}

fun test2(y: Int) {
    val <!UNUSED_VARIABLE!>x<!> = <!NO_ELSE_IN_WHEN!>when<!> (y) {
        1 -> true
    }
}

fun test3(x: Int): Boolean {
    run {
        when {
            x == 1 -> println()
            x < 0 -> {
                println()
                return true
            }
        }
    }
    return false
}
