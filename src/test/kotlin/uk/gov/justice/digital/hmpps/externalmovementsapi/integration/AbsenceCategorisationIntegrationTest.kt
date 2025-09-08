package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CodedDescription

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

  @Test
  fun `403 forbidden domain without correct role`() {
    getDomainSpec("any-domain", "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden reference data without correct role`() {
    getLinksSpec("any-domain", "anycode", "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 domain not found if invalid domain`() {
    getDomainSpec("any-domain").expectStatus().isNotFound
  }

  @Test
  fun `404 links not found if invalid domain`() {
    getDomainSpec("any-domain").expectStatus().isNotFound
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
      AbsenceCategorisation("SR", "Standard ROTL (Release on Temporary Licence)", null, ABSENCE_SUB_TYPE),
      AbsenceCategorisation("RR", "Restricted ROTL (Release on Temporary Licence)", null, ABSENCE_SUB_TYPE),
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
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(REFERENCE_DATA_URL, domain)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  private fun getLinksSpec(
    domain: String,
    code: String,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(LINKED_RD_URL, domain, code)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val REFERENCE_DATA_URL = "/absence-categorisation/{domain}"
    const val LINKED_RD_URL = "$REFERENCE_DATA_URL/{code}"

    @JvmStatic
    fun referenceDataDomains() = ReferenceDataDomain.Code.entries.map { rdd ->
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
            AbsenceCategorisation("LTX", "Court, legal, police or prison transfer", null, null),
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
              "CRL (Childcare Resettlement Licence)",
              "To help prisoners prepare for parenting when they are released and support ties between primary carers and their children.",
              null,
            ),
            AbsenceCategorisation(
              "RDR",
              "RDR (Resettlement Day Release)",
              "For prisoners to carry out activities linked to objectives in their sentence plan.",
              ABSENCE_REASON_CATEGORY,
            ),
            AbsenceCategorisation(
              "ROR",
              "ROR (Resettlement Overnight Release)",
              "For prisoners to spend time at their release address to re-establish links with family and the local community.",
              null,
            ),
            AbsenceCategorisation(
              "SPL",
              "SPL (Special Purpose Licence)",
              "For prisoners to spend time at their release address to re-establish links with family and the local community.",
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
              "CRL (Childcare Resettlement Licence)",
              "To help prisoners prepare for parenting when they are released and support ties between primary carers and their children.",
              null,
            ),
            AbsenceCategorisation(
              "RDR",
              "RDR (Resettlement Day Release)",
              "For prisoners to carry out activities linked to objectives in their sentence plan.",
              ABSENCE_REASON_CATEGORY,
            ),
            AbsenceCategorisation(
              "ROR",
              "ROR (Resettlement Overnight Release)",
              "For prisoners to spend time at their release address to re-establish links with family and the local community.",
              null,
            ),
            AbsenceCategorisation(
              "SPL",
              "SPL (Special Purpose Licence)",
              "For prisoners to spend time at their release address to re-establish links with family and the local community.",
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
            AbsenceCategorisation("C4", "Wedding or civil ceremony", null, null),
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
            AbsenceCategorisation("YRDR", "RDR (Resettlement Day Release)", null, null),
            AbsenceCategorisation("RO", "ROR (Resettlement Overnight Release)", null, null),
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
            AbsenceCategorisation("R16", "Paid work - Agriculture and horticulture", null, null),
            AbsenceCategorisation("R12", "Paid work - Catering and hospitality", null, null),
            AbsenceCategorisation("R14", "Paid work - Construction and recycling", null, null),
            AbsenceCategorisation("R15", "Paid work - IT and communication", null, null),
            AbsenceCategorisation("R11", "Paid work - Manufacturing", null, null),
            AbsenceCategorisation("R10", "Paid work - Retail and wholesale", null, null),
            AbsenceCategorisation("R13", "Paid work - Transportation and storage", null, null),
            AbsenceCategorisation("R17", "Paid work - Other", null, null),
          ),
        ),
      ),

      Arguments.of(
        "absence-reason-category",
        "UW",
        AbsenceCategorisations(
          CodedDescription("ABSENCE_REASON", "Absence reason"),
          listOf(
            AbsenceCategorisation("R24", "Unpaid work - Agriculture and horticulture", null, null),
            AbsenceCategorisation("R20", "Unpaid work - Catering and hospitality", null, null),
            AbsenceCategorisation("R22", "Unpaid work - Construction and recycling", null, null),
            AbsenceCategorisation("R23", "Unpaid work - IT and communication", null, null),
            AbsenceCategorisation("R19", "Unpaid work - Manufacturing", null, null),
            AbsenceCategorisation("R18", "Unpaid work - Retail and wholesale", null, null),
            AbsenceCategorisation("R21", "Unpaid work - Transportation and storage", null, null),
            AbsenceCategorisation("R25", "Unpaid work - Other", null, null),
          ),
        ),
      ),
    )
  }
}
