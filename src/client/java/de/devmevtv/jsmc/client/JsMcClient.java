package de.devmevtv.jsmc.client;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class JsMcClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            dispatcher.register(ClientCommandManager.literal("run").executes((context) -> {
                Javet.run();
                return Command.SINGLE_SUCCESS;
            }));
        });
    }
}
