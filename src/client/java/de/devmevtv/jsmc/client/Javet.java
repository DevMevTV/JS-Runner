package de.devmevtv.jsmc.client;

import com.caoccao.javet.enums.JSRuntimeType;
import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.converters.JavetProxyConverter;
import com.caoccao.javet.interop.engine.IJavetEngine;
import com.caoccao.javet.interop.engine.IJavetEnginePool;
import com.caoccao.javet.interop.engine.JavetEngineConfig;
import com.caoccao.javet.interop.engine.JavetEnginePool;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class Javet {
    public static void run() {
        new Thread(() -> {
            try (IJavetEnginePool<NodeRuntime> enginePool = new JavetEnginePool<>(
                    new JavetEngineConfig().setJSRuntimeType(JSRuntimeType.Node)
            )) {
                try (IJavetEngine<NodeRuntime> engine = enginePool.getEngine()) {
                    NodeRuntime runtime = engine.getV8Runtime();

                    runtime.setConverter(new JavetProxyConverter());

                    try (var globalObject = runtime.getGlobalObject()) {
                        globalObject.set("console", Console.class);
                        runtime.getExecutor(FabricLoader.getInstance().getGameDir().resolve("test").resolve("index.js")).executeVoid();
                        runtime.getGlobalObject().delete("console");
                    }
                }
            } catch (JavetException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public final class Console {
        public static void log(String msg) {
            MinecraftClient.getInstance().player.sendMessage(Text.of(msg), false);
        }
    }
}
