package com.example.template_mod.loader

import com.example.template_mod.client.TemplateModClient
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader

class FabricClientTemplateMod: ClientModInitializer {
    override fun onInitializeClient() {
        TemplateModClient.launchClient {
            loader = "fabric";
            isLoaded = FabricLoader.getInstance()
                .isModLoaded(TemplateModClient.MODID)
        }
    }
}