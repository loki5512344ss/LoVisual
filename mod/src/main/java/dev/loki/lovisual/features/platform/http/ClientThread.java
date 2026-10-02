package dev.loki.lovisual.features.platform.http;

import net.minecraft.client.Minecraft;

/** Hops a callback from the HTTP executor back onto the Minecraft client thread. */
public final class ClientThread {

    private ClientThread() {
    }

    public static void run(Runnable runnable) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.isSameThread()) {
            runnable.run();
        } else {
            mc.execute(runnable);
        }
    }
}
