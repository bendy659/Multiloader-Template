//? if fabric {
/*package com.example.template_mod.loader

import com.example.template_mod.ITemplateMod
import com.example.template_mod.client.TemplateModClient
import com.example.template_mod.common.TemplateMod
import com.example.template_mod.server.TemplateModServer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader

class FabricClientTemplateMod: ClientModInitializer {
    override fun onInitializeClient() {
        TemplateModClient.launch(
            ModPlatform(
                loader = "fabric",
                isLoaded = FabricLoader.getInstance().isModLoaded(ITemplateMod.MODID)
            )
        )
    }
}

class FabricTemplateMod: ModInitializer {
    override fun onInitialize() {
        TemplateMod.launch(
            ModPlatform(
                loader = "fabric",
                isLoaded = FabricLoader.getInstance().isModLoaded(ITemplateMod.MODID)
            )
        )
    }
}

class FabricServerTemplateMod: DedicatedServerModInitializer {
    override fun onInitializeServer() {
        TemplateModServer.launch(
            ModPlatform(
                loader = "fabric",
                isLoaded = FabricLoader.getInstance().isModLoaded(ITemplateMod.MODID)
            )
        )
    }
}
*///?}