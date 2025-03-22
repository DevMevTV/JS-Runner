package de.devmevtv.jsmc.client;

import com.caoccao.javet.enums.JSRuntimeType;
import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.converters.JavetProxyConverter;
import com.caoccao.javet.interop.engine.IJavetEngine;
import com.caoccao.javet.interop.engine.IJavetEnginePool;
import com.caoccao.javet.interop.engine.JavetEngineConfig;
import com.caoccao.javet.interop.engine.JavetEnginePool;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Javet {

    public static NodeRuntime runtime;

    static {
        init();
    }

    public static void init() {
        IJavetEnginePool<NodeRuntime> enginePool = new JavetEnginePool<>(
                new JavetEngineConfig().setJSRuntimeType(JSRuntimeType.Node)
        );

        try {
            runtime = enginePool.getEngine().getV8Runtime();

            runtime.setConverter(new JavetProxyConverter());
            runtime.getGlobalObject().set("console", Console.class);
            runtime.getGlobalObject().set("world", World.class);

        } catch (JavetException e) {
            Console.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void run(Path project) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject(Files.readString(project.resolve("package.json")));

                runtime.getExecutor(project.resolve(json.getString("main"))).executeVoid();
            } catch (JavetException e) {
                Console.error(e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                Console.error("Missing package.json in project");
                e.printStackTrace();
            }
        }).start();
    }

    public final class Console {
        public static void log(String msg) {
            MinecraftClient.getInstance().player.sendMessage(Text.of(msg), false);
        }

        public static void info(String msg) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal("[Info] " + msg).formatted(Formatting.GREEN), false);
        }

        public static void warn(String msg) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal("[Warn] " + msg).formatted(Formatting.YELLOW), false);
        }

        public static void error(String msg) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal("[Error] " + msg).formatted(Formatting.RED), false);
        }
    }

    public static final class World {
        private World() {}

        public static void write(String msg) {
            MinecraftClient.getInstance().getNetworkHandler().sendChatMessage(msg);
        }

        public static void onTick(Runnable callback) {
            JsMcClient.addEvent("Tick", callback);
        }
    }
}
