package de.devmevtv.jsmc.client;

import com.caoccao.javet.exceptions.JavetException;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JsMcClient implements ClientModInitializer {

    private static final Map<String, List<Runnable>> Events = new HashMap<>();
    public static Path ScriptsFolder;

    @Override
    public void onInitializeClient() {
        ScriptsFolder = FabricLoader.getInstance().getGameDir().resolve("jsrunner");

        try {
            if (!Files.exists(ScriptsFolder))
                Files.createDirectory(ScriptsFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {

            dispatcher.register(ClientCommandManager.literal("javascriptrunner").then(ClientCommandManager.literal("new").then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(context -> {
                String name = StringArgumentType.getString(context, "name");
                Path project = ScriptsFolder.resolve(name);
                if (Files.exists(project)) {
                    MinecraftClient.getInstance().player.sendMessage(Text.of("§cProject " + name + " already exists"), false);
                    return Command.SINGLE_SUCCESS;
                }
                try {
                    Files.createDirectory(project);
                    Files.writeString(project.resolve("main.js"), "console.log('Hello, World')");
                    Files.writeString(project.resolve("package.json"), "{" +
                            "\"name\":\"" + name + "\"," +
                            "\"version:\": \"1.0.0\"," +
                            "\"description\": \"\"," +
                            "\"main\":\"main.js\"" +
                            "\"scripts\": {" +
                            "\"    \"test\": \"echo \\\"Error: no test specified\\\" && exit 1\"" +
                            "\"}," +
                            "\"keywords\": []," +
                            "\"author\": \"\"," +
                            "\"license\": \"ISC\"" +
                            "}");
                } catch (IOException e) {
                    MinecraftClient.getInstance().player.sendMessage(Text.of("§cFailed to create project" + name), false);
                    e.printStackTrace();
                }
                MinecraftClient.getInstance().player.sendMessage(Text.of("§aSuccessfully created " + name), false);
                return Command.SINGLE_SUCCESS;
            }))).then(ClientCommandManager.literal("run").then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(context -> {
                String name = StringArgumentType.getString(context, "name");
                Path project = ScriptsFolder.resolve(name);
                if (!Files.exists(project)) {
                    MinecraftClient.getInstance().player.sendMessage(Text.of("§cProject " + name + " doesn't exist"), false);
                    return Command.SINGLE_SUCCESS;
                }
                Javet.run(project);
                return Command.SINGLE_SUCCESS;
            }))).then(ClientCommandManager.literal("stop").executes(context -> {
                try {
                    Javet.runtime.close(true);
                    Javet.runtime.removeCallbackContext(0);
                } catch (JavetException e) {
                    throw new RuntimeException(e);
                } finally {
                    Javet.init();
                }
                return Command.SINGLE_SUCCESS;
            })));
            dispatcher.register(ClientCommandManager.literal("jsrunner").redirect(dispatcher.findNode(Collections.singleton("javascriptrunner"))));
            dispatcher.register(ClientCommandManager.literal("jsr").redirect(dispatcher.findNode(Collections.singleton("javascriptrunner"))));
        });

        ClientTickEvents.START_CLIENT_TICK.register(a -> handleEvent("Tick"));
    }

    public static void handleEvent(String event) {
        Events.computeIfPresent(event, (key, list) -> {
            Events.get("Tick").forEach(Runnable::run);
            return list;
        });
    }

    public static void addEvent(String event, Runnable callback) {
        Events.computeIfAbsent(event, a -> new ArrayList<>());

        Events.get(event).add(callback);
    }
}
