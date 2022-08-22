package org.xapps.services.utils

import io.smallrye.jwt.build.Jwt
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import org.eclipse.microprofile.jwt.Claims





@ApplicationScoped
class TokenUtils @Inject constructor(
    @ConfigProperty(name = "security.private-key.location")
    private val privateKeyPath: String
) {

    private val privateKey = readPrivateKey(privateKeyPath)

    @Throws(java.lang.Exception::class)
    private fun readPrivateKey(privateKeyPath: String): PrivateKey {
        val path = Paths.get(TokenUtils::class.java.getResource(privateKeyPath)!!.toURI())
        val rawBase64Key = Files.readAllBytes(path)
        val base64Key = String(rawBase64Key, 0, rawBase64Key.size)
        val privateKeyPEM = base64Key
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace(System.lineSeparator().toRegex(), "")
            .replace("-----END PRIVATE KEY-----", "")
        val encoded: ByteArray = Base64.getDecoder().decode(privateKeyPEM)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(encoded)
        return keyFactory.generatePrivate(keySpec)
    }

    fun generateToken(username: String?, groups: Set<String>?, duration: Long, issuer: String?): String {
        val currentTimeInSecs = Instant.now().epochSecond
        val token: String = Jwt
            .issuer("https://example.com/issuer")
            .upn("jdoe@quarkus.io")
            .groups(HashSet(Arrays.asList("User", "Admin")))
//            .groups(groups)
//            .issuedAt(currentTimeInSecs)
//            .expiresAt(currentTimeInSecs + duration)
            .claim(Claims.birthdate.name, "2001-07-13")
            .sign()
        println(token)
        return token
//        val claimsBuilder = Jwt.claims()
//        val currentTimeInSecs = Instant.now().epochSecond
//        claimsBuilder.issuer(issuer)
//        claimsBuilder.subject(username)
//        claimsBuilder.issuedAt(currentTimeInSecs)
//        claimsBuilder.expiresAt(currentTimeInSecs + duration)
//        claimsBuilder.groups(groups)
//        return claimsBuilder.jws().keyId(privateKeyPath).sign(privateKey)
    }

}