package com.example.template_mod.server

import com.example.template_mod.ITemplateMod
import com.example.template_mod.loader.ModPlatform

object TemplateModServer: ITemplateMod {
    override lateinit var PLATFORM: ModPlatform

    override fun launch(nPlatform: ModPlatform) {
        super.launch(nPlatform)

        // Any code for "Server-Side"... //
    }
}