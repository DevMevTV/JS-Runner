package de.devmevtv.jsmc.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.mozilla.javascript.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

public class JsMcClient implements ClientModInitializer {

    public static Path ScriptsDir;

    public static void run(String fileName) {
        try {
            Context context = Context.enter();
            context.setLanguageVersion(Context.VERSION_ES6);
            try {
                Scriptable scope = context.initStandardObjects();

                ScriptableObject.putProperty(scope, "console", Context.javaToJS(new Console(), scope));

                String script = Files.readString(ScriptsDir.resolve(fileName + ".js")) + System.lineSeparator();
                context.evaluateString(scope, script, "userScript", 1, null);

            } catch (IOException e) {
                MinecraftClient.getInstance().player.sendMessage(Text.of("File not found: " + fileName + ".js"), false);
            } finally {
                Context.exit();
            }
        } catch (Exception e) {
            e.printStackTrace();
            MinecraftClient.getInstance().player.sendMessage(Text.of("Failed to run " + fileName), false);
        }

    }

    @Override
    public void onInitializeClient() {
        ScriptsDir = FabricLoader.getInstance().getGameDir().resolve("scripts");

        try {
            Files.createDirectories(ScriptsDir);
            Files.writeString(ScriptsDir.resolve("main.js"), "");
            Files.list(ScriptsDir).forEach(path -> System.out.println("Found: " + path.getFileName()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            dispatcher.register(ClientCommandManager.literal("run").then(ClientCommandManager.argument("file", StringArgumentType.word()).executes(context -> {
                var file = StringArgumentType.getString(context, "file");

                run(file);

                return Command.SINGLE_SUCCESS;
            })));
        });
    }

    public static class Console {
        public void log(String message) {
            MinecraftClient.getInstance().player.sendMessage(Text.of(message), false);
        }
    }
}
