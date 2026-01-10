//? if forge {
package com.example.template_mod.loader

import com.example.template_mod.ITemplateMod
import com.example.template_mod.client.TemplateModClient
import com.example.template_mod.common.TemplateMod
import com.example.template_mod.server.TemplateModServer
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.minecraftforge.fml.loading.FMLEnvironment
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(ITemplateMod.MODID)
class ForgeTemplateMod {
    init {
        // For client side //
        if (FMLEnvironment.dist.isClient)
            MOD_BUS.addListener<FMLClientSetupEvent> { onClientSetup }

        // For common side //
        MOD_BUS.addListener<FMLCommonSetupEvent> { onCommonSetup }

        // For server side //
        if (FMLEnvironment.dist.isDedicatedServer)
            MOD_BUS.addListener<FMLDedicatedServerSetupEvent> { onServerSetup }
    }

    val onClientSetup: () -> Unit get() = {
        val forgeClientPlatform = ModPlatform(
            loader = "forge",
            isLoaded = ModList.get().isLoaded(ITemplateMod.MODID)
        )

        TemplateModClient.launch(forgeClientPlatform)
    }

    val onCommonSetup: () -> Unit get() = {
        val forgePlatform = ModPlatform(
            loader = "forge",
            isLoaded = ModList.get().isLoaded(ITemplateMod.MODID)
        )

        TemplateMod.launch(forgePlatform)
    }

    val onServerSetup: () -> Unit get() = {
        val forgeServerPlatform = ModPlatform(
            loader = "forge",
            isLoaded = ModList.get().isLoaded(ITemplateMod.MODID)
        )

        TemplateModServer.launch(forgeServerPlatform)
    }
}
//?}