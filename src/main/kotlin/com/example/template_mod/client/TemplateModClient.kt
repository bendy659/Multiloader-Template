package com.example.template_mod.client

import com.example.template_mod.ITemplateMod
import com.example.template_mod.loader.ModPlatform

object TemplateModClient: ITemplateMod {
    override lateinit var PLATFORM: ModPlatform

    override val isClientSide: Boolean get() = true

    override fun launch(nPlatform: ModPlatform) {
        super.launch(nPlatform)

        // Any code for "Client-Side"... //
    }
}