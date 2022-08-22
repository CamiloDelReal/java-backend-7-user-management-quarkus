package org.xapps.services.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.skydoves.whatif.whatIf
import io.smallrye.jwt.build.Jwt
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Instant
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class TokenUtils @Inject constructor(
        private val objectMapper: ObjectMapper,

        @ConfigProperty(name = "mp.jwt.verify.issuer")
        private val issuer: String,

        @ConfigProperty(name = "security.token.expiration")
        private val expiration: Long
) {
    fun generateToken(username: String?, groups: Set<String>?, subject: Any?): String {
        val currentTimeInSecs = Instant.now().epochSecond
        return Jwt
                .upn(username)
                .issuer(issuer)
                .whatIf(groups != null) {
                    groups(groups)
                }
                .whatIf(subject != null) {
                    subject(objectMapper.writeValueAsString(subject))
                }
                .issuedAt(currentTimeInSecs)
                .expiresAt(currentTimeInSecs + expiration)
                .sign()
    }

}