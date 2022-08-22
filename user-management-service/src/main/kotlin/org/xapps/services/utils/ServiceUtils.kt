package org.xapps.services.utils

import com.fasterxml.jackson.databind.ObjectMapper
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.Produces

@ApplicationScoped
class ServiceUtils {

    @Produces
    fun provideObjectMapper(): ObjectMapper =
        ObjectMapper()

}