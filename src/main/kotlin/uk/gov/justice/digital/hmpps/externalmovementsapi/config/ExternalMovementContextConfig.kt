package uk.gov.justice.digital.hmpps.externalmovementsapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ValidationException
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set

@Configuration
class ExternalMovementContextConfiguration(private val contextInterceptor: ExternalMovementContextInterceptor) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry
      .addInterceptor(contextInterceptor)
      .addPathPatterns("/**")
      .excludePathPatterns(
        "/queue-admin/retry-all-dlqs",
        "/health/**",
        "/info",
        "/ping",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-resources/**",
      )
  }
}

@Configuration
class ExternalMovementContextInterceptor : HandlerInterceptor {
  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
  ): Boolean {
    ExternalMovementContext(getUsername()).set()
    return true
  }

  override fun postHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
    modelAndView: ModelAndView?,
  ) {
    ExternalMovementContext.clear()
    super.postHandle(request, response, handler, modelAndView)
  }

  private fun getUsername(): String = SecurityContextHolder
    .getContext()
    .authentication
    .name
    .trim()
    .takeUnless(String::isBlank)
    ?.also { if (it.length > 64) throw ValidationException("Username must be <= 64 characters") }
    ?: throw ValidationException("Could not find non empty username")
}
