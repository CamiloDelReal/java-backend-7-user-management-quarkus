package org.xapps.services.resources

import io.quarkus.security.Authenticated
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.jwt.JsonWebToken
import org.xapps.services.entities.Credential
import org.xapps.services.entities.Role
import org.xapps.services.entities.User
import org.xapps.services.services.UserService
import javax.annotation.security.PermitAll
import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext


@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class UserResource @Inject constructor(
    private val userService: UserService
) {
    @POST
    @PermitAll
    @Path("/login")
    fun login(credential: Credential): Response =
        try {
            userService.login(credential)?.let { authentication ->
                Response.ok(authentication).build()
            } ?: run {
                Response.status(Response.Status.UNAUTHORIZED).build()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }


    @Inject
    var jwt: JsonWebToken? = null
    @GET
    @Path("/hello")
//    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    fun hello(@Context ctx: SecurityContext): String? {
        println("/hello endpoint")
        return getResponseString(ctx)
    }

    private fun getResponseString(ctx: SecurityContext): String? {
        val name: String = if (ctx.userPrincipal == null) {
            "anonymous"
        } else if (!ctx.userPrincipal.name.equals(jwt?.name)) {
            throw InternalServerErrorException("Principal and JsonWebToken names do not match")
        } else {
            ctx.userPrincipal.name
        }
        return java.lang.String.format(
            "hello + %s,"
                    + " isHttps: %s,"
                    + " authScheme: %s,"
                    + " hasJWT: %s",
            name, ctx.isSecure, ctx.authenticationScheme, hasJwt()
        )
    }

    private fun hasJwt(): Boolean {
        return jwt?.claimNames != null
    }
//    @GET
//    @RolesAllowed(Role.ADMINISTRATOR)
//    fun getAllUsers(): Response =
//        Response.ok(userService.findAll()).build()

//    @GET
//    @Path("/{id}")
//    @RolesAllowed(Role.ADMINISTRATOR)
//    fun getUserById(id: Long): Response =
//        try {
//            userService.findById(id)?.let { user ->
//                Response.ok(user).status(Response.Status.CREATED).build()
//            } ?: run {
//                Response.status(Response.Status.NOT_FOUND).build()
//            }
//        } catch (ex: Exception) {
//            Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
//        }
//
//    @POST
//    @PermitAll
//    fun createUser(user: User): Response =
//        Response.ok(userService.create(user)).build()
//
//    @PUT
//    @Path("/{id}")
//    @Authenticated
//    fun updateUser(id: Long, user: User): Response =
//        userService.update(id, user)?.let { userPersisted ->
//            Response.ok(userPersisted).build()
//        } ?: run {
//            Response.status(Response.Status.NOT_FOUND).build()
//        }
//
//    @DELETE
//    @Path("/{id}")
//    @Authenticated
//    fun deleteUser(id: Long): Response =
//        if(userService.delete(id)) {
//            Response.ok().build()
//        } else {
//            Response.status(Response.Status.NOT_FOUND).build()
//        }

}