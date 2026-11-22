package dev.mastodon.streaming.util

/**
 * filter.filter_action is the value from the custom_filters.action database
 * column, it is an integer representing a value in an enum defined by Ruby on
 * Rails:
 *
 * warn: 0, hide: 1
 */
object Permissions {
    const val VIEW_FEEDS: Long = 0x0_000_000_000_100_000
}
