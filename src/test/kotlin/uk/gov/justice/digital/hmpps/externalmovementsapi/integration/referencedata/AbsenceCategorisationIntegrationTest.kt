package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.referencedata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription

class AbsenceCategorisationIntegrationTest : IntegrationTest() {
  @Test
  fun `401 unauthorised domain without a valid token`() {
    webTestClient
      .get()
      .uri(REFERENCE_DATA_URL, "any-domain")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `401 unauthorised reference data without a valid token`() {
    webTestClient
      .get()
      .uri(LINKED_RD_URL, "any-domain", "ANY_CODE")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI, "ROLE_ANY__OTHER_RW"])
  fun `403 forbidden domain without correct role`(role: String) {
    getDomainSpec("any-domain", role).expectStatus().isForbidden
  }

  @ParameterizedTest
  @ValueSource(strings = [EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI, "ROLE_ANY__OTHER_RW"])
  fun `403 forbidden reference data without correct role`(role: String) {
    getLinksSpec("any-domain", "anycode", role).expectStatus().isForbidden
  }

  @Test
  fun `404 domain not found if invalid domain`() {
    getDomainSpec("any-domain").expectStatus().isNotFound
  }

  @Test
  fun `404 links not found if invalid domain`() {
    getLinksSpec("any-domain", "anycode").expectStatus().isNotFound
  }

  @Test
  fun `200 ok - can retrieve absence types`() {
    val rd =
      getDomainSpec("absence-type")
        .expectStatus()
        .isOk
        .expectBody<AbsenceCategorisations>()
        .returnResult()
        .responseBody!!

    assertThat(rd.domain).isEqualTo(CodedDescription("ABSENCE_TYPE", "Absence type"))
    assertThat(rd.items).containsExactly(
      AbsenceCategorisation("SR", "Standard ROTL (release on temporary licence)", null, ABSENCE_SUB_TYPE),
      AbsenceCategorisation("RR", "Restricted ROTL (release on temporary licence)", null, ABSENCE_SUB_TYPE),
      AbsenceCategorisation("PP", "Police production", null, null),
      AbsenceCategorisation("SE", "Security escort", null, ABSENCE_REASON),
      AbsenceCategorisation("YT", "Youth temporary release", null, ABSENCE_SUB_TYPE),
    )
  }

  @ParameterizedTest
  @MethodSource("referenceDataDomains")
  fun `200 ok - can retrieve reference data domains with correct role`(domain: String) {
    val rd =
      getDomainSpec(domain)
        .expectStatus()
        .isOk
        .expectBody<AbsenceCategorisations>()
        .returnResult()
        .responseBody!!

    assertThat(rd.items).isNotEmpty
  }

  @ParameterizedTest
  @MethodSource("linkedReferenceData")
  fun `200 ok - can retrieve linked reference data`(domainCode: String, rdCode: String, ac: AbsenceCategorisations) {
    val res =
      getLinksSpec(domainCode, rdCode)
        .expectStatus()
        .isOk
        .expectBody<AbsenceCategorisations>()
        .returnResult()
        .responseBody!!

    assertThat(res).isEqualTo(ac)
  }

  @Test
  fun `204 no content when no linked items`() {
    getLinksSpec("absence-type", "PP").expectStatus().isNoContent
  }

  private fun getDomainSpec(
    domain: String,
    role: String? = listOf(Roles.TEMPORARY_ABSENCE_RO, Roles.TEMPORARY_ABSENCE_RW).random(),
  ) = webTestClient
    .get()
    .uri(REFERENCE_DATA_URL, domain)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  private fun getLinksSpec(
    domain: String,
    code: String,
    role: String? = listOf(Roles.TEMPORARY_ABSENCE_RO, Roles.TEMPORARY_ABSENCE_RW).random(),
  ) = webTestClient
    .get()
    .uri(LINKED_RD_URL, domain, code)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val REFERENCE_DATA_URL = "/absence-categorisation/{domain}"
    const val LINKED_RD_URL = "$REFERENCE_DATA_URL/{code}"

    @JvmStatic
    fun referenceDataDomains() = listOf(ABSENCE_TYPE, ABSENCE_SUB_TYPE, ABSENCE_REASON_CATEGORY, ABSENCE_REASON).map { rdd ->
      Arguments.of(rdd.name)
      Arguments.of(rdd.name.lowercase())
      Arguments.of(rdd.name.lowercase().replace("_", "-"))
    }

    @JvmStatic
    fun linkedReferenceData() = listOf(
      Arguments.of(
        "absence-type",
        "SE",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_REASON", "Absence reason"),
          listOf(
            AbsenceCategorisation("C3", "Death or funeral", null, null),
            AbsenceCategorisation("C6", "Inpatient medical or dental appointment", null, null),
            AbsenceCategorisation("C5", "Outpatient medical or dental appointment", null, null),
            AbsenceCategorisation("C7", "Visit a dying relative", null, null),
            AbsenceCategorisation("C4", "Wedding or civil ceremony", null, null),
            AbsenceCategorisation("4", "Other compassionate reason", null, null),
            AbsenceCategorisation("SE", "Other security escort", null, null),
          ),
        ),
      ),

      Arguments.of(
        "absence-type",
        "RR",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_SUB_TYPE", "Absence sub type"),
          listOf(
            AbsenceCategorisation(
              "CRL",
              "CRL (childcare resettlement licence)",
              "To help prisoners prepare for parenting when they are released and support ties between primary carers and their children.",
              null,
            ),
            AbsenceCategorisation(
              "RDR",
              "RDR (resettlement day release)",
              "For prisoners to carry out activities linked to objectives in their sentence plan.",
              ABSENCE_REASON_CATEGORY,
            ),
            AbsenceCategorisation(
              "ROR",
              "ROR (resettlement overnight release)",
              "For prisoners to spend time at their release address to re-establish links with family and the local community.",
              null,
            ),
            AbsenceCategorisation(
              "SPL",
              "SPL (special purpose licence)",
              "A short release in response to exceptional or personal circumstances, such as medical appointments, or wider criminal justice needs.",
              ABSENCE_REASON,
            ),
          ),
        ),
      ),

      Arguments.of(
        "absence-type",
        "SR",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_SUB_TYPE", "Absence sub type"),
          listOf(
            AbsenceCategorisation(
              "CRL",
              "CRL (childcare resettlement licence)",
              "To help prisoners prepare for parenting when they are released and support ties between primary carers and their children.",
              null,
            ),
            AbsenceCategorisation(
              "RDR",
              "RDR (resettlement day release)",
              "For prisoners to carry out activities linked to objectives in their sentence plan.",
              ABSENCE_REASON_CATEGORY,
            ),
            AbsenceCategorisation(
              "ROR",
              "ROR (resettlement overnight release)",
              "For prisoners to spend time at their release address to re-establish links with family and the local community.",
              null,
            ),
            AbsenceCategorisation(
              "SPL",
              "SPL (special purpose licence)",
              "A short release in response to exceptional or personal circumstances, such as medical appointments, or wider criminal justice needs.",
              ABSENCE_REASON,
            ),
          ),
        ),
      ),

      Arguments.of(
        "absence-type",
        "YT",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_SUB_TYPE", "Absence sub type"),
          listOf(
            AbsenceCategorisation("YTRA", "Accommodation", null, ABSENCE_REASON),
            AbsenceCategorisation("YTRC", "Case work and transitions", null, ABSENCE_REASON),
            AbsenceCategorisation("YTRE", "Education, training and employment", null, ABSENCE_REASON),
            AbsenceCategorisation("YTRF", "Family", null, ABSENCE_REASON),
          ),
        ),
      ),

      Arguments.of(
        "absence-sub-type",
        "RDR",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_REASON_CATEGORY", "Absence reason category"),
          listOf(
            AbsenceCategorisation("FB", "Accommodation-related", null, null),
            AbsenceCategorisation("ET", "Education or training", null, null),
            AbsenceCategorisation("R3", "Maintaining family ties", null, null),
            AbsenceCategorisation("PW", "Paid work", null, ABSENCE_REASON),
            AbsenceCategorisation("PAP", "Prisoner apprenticeships pathway", null, null),
            AbsenceCategorisation("UW", "Unpaid work", null, ABSENCE_REASON),
            AbsenceCategorisation(
              "YOTR",
              "Other temporary release linked to sentence or resettlement plan",
              null,
              null,
            ),
          ),
        ),
      ),

      Arguments.of(
        "absence-sub-type",
        "SPL",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_REASON", "Absence reason"),
          listOf(
            AbsenceCategorisation("LTX", "Court, legal, police or prison transfer", null, null),
            AbsenceCategorisation("C3", "Death or funeral", null, null),
            AbsenceCategorisation("C6", "Inpatient medical or dental appointment", null, null),
            AbsenceCategorisation("C5", "Outpatient medical or dental appointment", null, null),
            AbsenceCategorisation("C7", "Visit a dying relative", null, null),
            AbsenceCategorisation("4", "Other compassionate reason", null, null),
          ),
        ),
      ),

      Arguments.of(
        "absence-sub-type",
        "YTRA",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_REASON", "Absence reason"),
          listOf(
            AbsenceCategorisation("YRDR", "RDR (resettlement day release)", null, null),
            AbsenceCategorisation("RO", "ROR (resettlement overnight release)", null, null),
          ),
        ),
      ),

      Arguments.of(
        "absence-sub-type",
        "YTRC",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_REASON", "Absence reason"),
          listOf(
            AbsenceCategorisation("YMI", "Meeting and interviews", null, null),
            AbsenceCategorisation("20", "Other temporary release linked to sentence or resettlement plan", null, null),
            AbsenceCategorisation("OPA", "Outside prison activity", null, null),
          ),
        ),
      ),

      Arguments.of(
        "absence-sub-type",
        "YTRC",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_REASON", "Absence reason"),
          listOf(
            AbsenceCategorisation("YMI", "Meeting and interviews", null, null),
            AbsenceCategorisation("20", "Other temporary release linked to sentence or resettlement plan", null, null),
            AbsenceCategorisation("OPA", "Outside prison activity", null, null),
          ),
        ),
      ),

      Arguments.of(
        "absence-sub-type",
        "YTRE",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_REASON", "Absence reason"),
          listOf(
            AbsenceCategorisation("ET", "Education or training", null, null),
            AbsenceCategorisation("PAP", "Prisoner apprenticeships pathway", null, null),
          ),
        ),
      ),

      Arguments.of(
        "absence-sub-type",
        "YTRF",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_REASON", "Absence reason"),
          listOf(
            AbsenceCategorisation("C3", "Death or funeral", null, null),
            AbsenceCategorisation("R3", "Maintaining family ties", null, null),
            AbsenceCategorisation("C7", "Visit a dying relative", null, null),
            AbsenceCategorisation("C4", "Wedding or civil ceremony", null, null),
            AbsenceCategorisation("4", "Other compassionate reason", null, null),
          ),
        ),
      ),

      Arguments.of(
        "absence-reason-category",
        "PW",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_REASON", "Absence reason"),
          listOf(
            AbsenceCategorisation("R16", "Agriculture and horticulture", null, null),
            AbsenceCategorisation("R12", "Catering and hospitality", null, null),
            AbsenceCategorisation("R14", "Construction and recycling", null, null),
            AbsenceCategorisation("R15", "IT and communication", null, null),
            AbsenceCategorisation("R11", "Manufacturing", null, null),
            AbsenceCategorisation("R10", "Retail and wholesale", null, null),
            AbsenceCategorisation("R13", "Transportation and storage", null, null),
            AbsenceCategorisation("R17", "Other", null, null),
          ),
        ),
      ),

      Arguments.of(
        "absence-reason-category",
        "UW",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_REASON", "Absence reason"),
          listOf(
            AbsenceCategorisation("R24", "Agriculture and horticulture", null, null),
            AbsenceCategorisation("R20", "Catering and hospitality", null, null),
            AbsenceCategorisation("R22", "Construction and recycling", null, null),
            AbsenceCategorisation("R23", "IT and communication", null, null),
            AbsenceCategorisation("R19", "Manufacturing", null, null),
            AbsenceCategorisation("R18", "Retail and wholesale", null, null),
            AbsenceCategorisation("R21", "Transportation and storage", null, null),
            AbsenceCategorisation("R25", "Other", null, null),
          ),
        ),
      ),
    )
  }
}
