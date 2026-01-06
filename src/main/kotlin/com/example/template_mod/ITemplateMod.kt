package com.example.template_mod

import com.example.template_mod.loader.ModPlatform
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface ITemplateMod {
    companion object { const val MODID: String = "template_mod" }
    val LOGGER: Logger get() = LoggerFactory.getLogger(MODID)
    var PLATFORM: ModPlatform

    val isClientSide: Boolean get() = false

    fun launch(nPlatform: ModPlatform) {
        PLATFORM = nPlatform; LOGGER.info("Now launch in '${PLATFORM.loader}${if (isClientSide) " client" else ""}!")
    }
}