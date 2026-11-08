package dev.mastodon.streaming.util.primitives

enum class RegexPattern(val regex: Regex) {
    SPECIAL_CHARS(Regex("""[.*+?^${'$'}{}()|\[\]\\]""")),
    STARTS_WITH_WORD_CHAR(Regex("""^\w""")),
    ENDS_WITH_WORD_CHAR(Regex("""\w$""")),
    BR_TAG(Regex("""<br\s*/?>""")),
}