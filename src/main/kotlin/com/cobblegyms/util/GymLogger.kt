package com.cobblegyms.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object GymLogger {
    fun create(modId: String): Logger = LoggerFactory.getLogger(modId)
}
