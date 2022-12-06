package org.xapps.services.resources

import io.quarkus.security.Authenticated
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal
import org.xapps.services.entities.Credential
import org.xapps.services.entities.Role
import org.xapps.services.entities.User
import org.xapps.services.services.UserService
import org.xapps.services.services.exceptions.UserNotFoundException
import org.xapps.services.services.exceptions.ValidationException
import javax.annotation.security.PermitAll
import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.ws.rs.*
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
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ex.localizedMessage)
                .build()
        }

    private fun isUserAllowedToCreate(securityContext: SecurityContext, user: User, onAccept: () -> Response): Response =
        if(!userService.hasAdministratorRole(user) ||
                (securityContext.userPrincipal != null && userService.isAdministrator(userService.buildUser((securityContext.userPrincipal as DefaultJWTCallerPrincipal).subject)))) {
            onAccept.invoke()
        } else {
            Response.status(Response.Status.FORBIDDEN).build()
        }

    private fun isUserAllowedToReadOrUpdateOrDelete(securityContext: SecurityContext, id: Long, user: User? = null, onAccept: () -> Response): Response =
        if (securityContext.userPrincipal == null) {
            Response.status(Response.Status.UNAUTHORIZED).build()
        } else {
            val authenticatedUser = userService.buildUser((securityContext.userPrincipal as DefaultJWTCallerPrincipal).subject)
            if(userService.isAdministrator(authenticatedUser) || (id == authenticatedUser.id && !userService.hasAdministratorRole(user))) {
                onAccept.invoke()
            } else {
                Response.status(Response.Status.FORBIDDEN).build()
            }
        }

    @GET
    @PermitAll
    @Path("/roles")
    fun getAllRoles(): Response =
            try {
                Response.ok(userService.findAllRoles()).build()
            } catch (ex: Exception) {
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(ex.localizedMessage)
                        .build()
            }


    @GET
    @RolesAllowed(Role.ADMINISTRATOR)
    fun getAllUsers(): Response =
        try {
            Response.ok(userService.findAll()).build()
        } catch (ex: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ex.localizedMessage)
                .build()
        }

    @GET
    @Path("/{id}")
    @Authenticated
    fun getUserById(id: Long, securityContext: SecurityContext): Response =
        try {
            isUserAllowedToReadOrUpdateOrDelete(securityContext, id) {
                userService.findById(id)?.let { user ->
                    Response.ok(user).status(Response.Status.OK).build()
                } ?: run {
                    Response.status(Response.Status.NOT_FOUND).build()
                }
            }
        } catch (ex: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ex.localizedMessage)
                .build()
        }

    @POST
    @PermitAll
    fun createUser(user: User, securityContext: SecurityContext): Response =
        try {
            isUserAllowedToCreate(securityContext, user) {
                Response.ok(userService.create(user)).build()
            }
        } catch (ex: ValidationException) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(ex.message)
                .build()
        } catch (ex: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ex.localizedMessage)
                    .build()
        }

    @PUT
    @Path("/{id}")
    @Authenticated
    fun updateUser(id: Long, user: User, securityContext: SecurityContext): Response =
        try {
            isUserAllowedToReadOrUpdateOrDelete(securityContext, id, user) {
                Response.ok(userService.update(id, user)).build()
            }
        } catch (ex: UserNotFoundException) {
            Response.status(Response.Status.NOT_FOUND).build()
        } catch (ex: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ex.localizedMessage)
                .build()
        }

    @DELETE
    @Path("/{id}")
    @Authenticated
    fun deleteUser(id: Long, securityContext: SecurityContext): Response =
        try {
            isUserAllowedToReadOrUpdateOrDelete(securityContext, id) {
                if (userService.delete(id)) {
                    Response.ok().build()
                } else {
                    Response.status(Response.Status.NOT_MODIFIED).build()
                }
            }
        } catch (ex: UserNotFoundException) {
            Response.status(Response.Status.NOT_FOUND).build()
        } catch (ex: Exception) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ex.localizedMessage)
                .build()
        }

}


