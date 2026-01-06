//? if fabric {
/*package com.example.template_mod.loader

import com.example.template_mod.client.TemplateModClient
import com.example.template_mod.common.TemplateMod
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
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

class FabricTemplateMod: ModInitializer {
    override fun onInitialize() {
        TemplateMod.launch {
            loader = "fabric"
            isLoaded = FabricLoader.getInstance()
                .isModLoaded(TemplateMod.MODID)
        }
    }
}
*///?}