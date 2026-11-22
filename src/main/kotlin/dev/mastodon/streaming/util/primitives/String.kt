package dev.mastodon.streaming.util.primitives

import java.text.Normalizer

/**
 * See app/lib/ascii_folder.rb for the canon definitions
 * of these constants
 */
private const val NON_ASCII_CHARS = "ÀÁÂÃÄÅàáâãäåĀāĂăĄąÇçĆćĈĉĊċČčÐðĎďĐđÈÉÊËèéêëĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħÌÍÎÏìíîïĨĩĪīĬĭĮįİıĴĵĶķĸĹĺĻ" +
        "ļĽľĿŀŁłÑñŃńŅņŇňŉŊŋÒÓÔÕÖØòóôõöøŌōŎŏŐőŔŕŖŗŘřŚśŜŝŞşŠšſŢţŤťŦŧÙÚÛÜùúûüŨũŪūŬŭŮůŰűŲųŴŵÝýÿŶŷŸŹźŻżŽž"
private const val EQUIVALENT_ASCII_CHARS = "AAAAAAaaaaaaAaAaAaCcCcCcCcCcDdDdDdEEEEeeeeEeEeEeEeEeGgGgGgGgHhHhIIIIiiiiIiIiIiIiIiJjKkkLlL" +
        "lLlLlLlNnNnNnNnnNnOOOOOOooooooOoOoOoRrRrRrSsSsSsSssTtTtTtUUUUuuuuUuUuUuUuUuUuWwYyyYyYZzZzZz"

fun String.foldToAscii(): String = map { char ->
    val index = NON_ASCII_CHARS.indexOf(char)
    if (index >= 0) EQUIVALENT_ASCII_CHARS[index] else char
}.joinToString("")

fun String.normalizeHashtag(): String = Normalizer.normalize(this, Normalizer.Form.NFKC)
    .lowercase()
    .foldToAscii()
    .replace(Regex("""[^\p{L}\p{N}_\u00b7\u200c]"""), "")
