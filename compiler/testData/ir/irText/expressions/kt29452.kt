fun test1(x: Int) {
    when (x) {
        0 -> throw Error()
    }
}

fun test2(x: Int) {
    if (x == 0) throw Error()
}