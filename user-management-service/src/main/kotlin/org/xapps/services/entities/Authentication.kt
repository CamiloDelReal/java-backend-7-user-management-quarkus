package org.xapps.services.entities

data class Authentication(
    val username: String,
    val token: String,
    val expiration: Long
)