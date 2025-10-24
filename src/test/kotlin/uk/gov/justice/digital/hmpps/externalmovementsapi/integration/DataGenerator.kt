package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import java.time.LocalDate
import java.time.Month
import java.util.concurrent.atomic.AtomicLong

object DataGenerator {
  private val id = AtomicLong(1)
  private val letters = ('A'..'Z')
  private val years = LocalDate.now().year - 70..LocalDate.now().year - 18

  fun newId(): Long = id.getAndIncrement()
  fun personIdentifier(): String = "${letters.random()}${(1111..9999).random()}${letters.random()}${letters.random()}"
  fun name(length: Int): String = (1..length).joinToString("") { if (it == 1) letters.random().uppercase() else letters.random().lowercase() }

  fun dob(): LocalDate {
    val month = Month.entries.random()
    val day = (1..28).random()
    return LocalDate.of(years.random(), month, day)
  }

  fun username(): String = (0..12).joinToString("") { letters.random().toString() }
  fun cellLocation(): String = "${letters.random()}-${(1..9).random()}-${(111..999).random()}"
  fun prisonCode(): String = (1..3).map { letters.random() }.joinToString("")
  fun postcode(): String = ((1..2).map { letters.random() } + (1..3).map { (1..9).random() } + (1..2).map { letters.random() }).joinToString("")
}
