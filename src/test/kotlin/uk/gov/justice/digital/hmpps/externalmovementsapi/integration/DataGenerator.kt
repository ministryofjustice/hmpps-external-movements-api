package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import java.util.concurrent.atomic.AtomicLong

object DataGenerator {
  private val id = AtomicLong(1)
  private val letters = ('A'..'Z')

  fun newId(): Long = id.getAndIncrement()
  fun personIdentifier(): String = "${letters.random()}${(1111..9999).random()}${letters.random()}${letters.random()}"
  fun name(length: Int): String = (1..length).joinToString("") { if (it == 1) letters.random().uppercase() else letters.random().lowercase() }

  fun username(): String = (0..12).joinToString("") { letters.random().toString() }
  fun cellLocation(): String = "${letters.random()}-${(1..9).random()}-${(111..999).random()}"
}
