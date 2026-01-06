package com.example.template_mod.common

import com.example.template_mod.ITemplateMod
import com.example.template_mod.loader.ModPlatform

object TemplateMod: ITemplateMod {
    override lateinit var PLATFORM: ModPlatform

    override fun launch(nPlatform: ModPlatform) {
        super.launch(nPlatform)

        // Any code for "Common-Side"... //
    }
}