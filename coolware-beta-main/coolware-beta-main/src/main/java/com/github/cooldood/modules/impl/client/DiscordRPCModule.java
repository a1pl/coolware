package com.github.cooldood.modules.impl.client;

import com.github.cooldood.modules.*;
import com.github.cooldood.utils.client.C;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.minecraft.client.multiplayer.ServerData;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RegisterModule(
        name = "Discord RPC",
        description = "Shows Coolware rich presence in Discord.",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class DiscordRPCModule extends Module {
    private static final String APPLICATION_ID = "1550053383405051984";
    private static final String LARGE_IMAGE_KEY = "logo";
    private static final String LARGE_IMAGE_TEXT = "Coolware Client";

    @RegisterSubModule(name = "Show Server", description = "Displays current server IP or singleplayer in RPC")
    public static boolean showServer = true;

    @RegisterSubModule(name = "Show Timestamp", description = "Displays elapsed time in RPC")
    public static boolean showTimestamp = true;

    @RegisterSubModule(name = "Show Username", description = "Displays your player name in RPC")
    public static boolean showUsername = true;

    @RegisterSubModule(name = "Override Launcher", description = "Overrides Feather, Lunar, and launcher Discord RPCs")
    public static boolean overrideLauncher = true;

    private static DiscordRichPresence presence;
    private static ExecutorService executor;
    private static volatile boolean running = false;
    private static long startTimestamp = 0;

    private static String lastState = "";
    private static String lastDetails = "";
    private static boolean proxyInstalled = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(DiscordRPCModule::stopRPC, "Coolware-DiscordRPC-Shutdown"));
    }

    @Override
    protected void onEnable() {
        startRPC();
    }

    @Override
    protected void onDisable() {
        stopRPC();
    }

    public static void disableSKLauncher() {
        if (!overrideLauncher) return;
        try {
            Class<?> rpClass = Class.forName("com.sklauncher.injector.RichPresence");
            for (Method m : rpClass.getDeclaredMethods()) {
                if (m.getName().equalsIgnoreCase("shutdown")) {
                    m.setAccessible(true);
                    m.invoke(null);
                }
            }
            Field threadField = rpClass.getDeclaredField("discordThread");
            threadField.setAccessible(true);
            Object discordThread = threadField.get(null);
            if (discordThread != null) {
                for (Method m : discordThread.getClass().getDeclaredMethods()) {
                    if (m.getName().equalsIgnoreCase("terminate")) {
                        m.setAccessible(true);
                        m.invoke(discordThread);
                    }
                }
                threadField.set(null, null);
            }
            Field execField = rpClass.getDeclaredField("executorService");
            execField.setAccessible(true);
            Object exec = execField.get(null);
            if (exec instanceof java.util.concurrent.ExecutorService) {
                ((java.util.concurrent.ExecutorService) exec).shutdownNow();
                execField.set(null, null);
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> matClass = Class.forName("com.sklauncher.injector.transformers.MainArgumentsTransformer");
            Method getListeners = matClass.getMethod("getUsernameListeners");
            Object listeners = getListeners.invoke(null);
            if (listeners instanceof java.util.List) {
                ((java.util.List<?>) listeners).clear();
            }
        } catch (Throwable ignored) {}
    }

    public static void suppressRivalThreads() {
        if (!overrideLauncher) return;
        disableSKLauncher();
        try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t == null) continue;
                String name = t.getName();
                if (name == null) continue;
                if ("Coolware-Discord-RPC".equals(name) || "Coolware-DiscordRPC-Shutdown".equals(name)) {
                    continue;
                }
                String lowerName = name.toLowerCase();
                String className = t.getClass().getName().toLowerCase();
                if (lowerName.contains("discord") || lowerName.contains("rpc") || lowerName.contains("feather")
                        || className.contains("discord") || className.contains("drpc")
                        || className.contains("feather") || className.contains("essential")
                        || className.contains("lunar") || className.contains("badlion")
                        || className.contains("sklauncher")) {
                    try {
                        t.suspend();
                    } catch (Throwable ignored) {
                        try {
                            t.interrupt();
                        } catch (Throwable ignored2) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void disableLauncherMods() {
        if (!overrideLauncher) return;
        disableSKLauncher();
        String[] modClasses = new String[] {
            "com.sklauncher.injector.RichPresence",
            "net.digitalingot.feather.mods.DiscordMod",
            "net.digitalingot.feather.mods.DiscordRPMod",
            "net.digitalingot.feather.featherrpc.FeatherRPC",
            "gg.essential.handlers.discord.DiscordIntegration",
            "com.moonsworth.lunar.client.feature.type.discord.DiscordRPMod",
            "net.labymod.core.DiscordRPC",
            "com.gitlab.cdagaming.craftpresence.CraftPresence"
        };

        for (String clsName : modClasses) {
            try {
                Class<?> clazz = Class.forName(clsName);
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getParameterTypes().length == 0 &&
                        (m.getName().equalsIgnoreCase("disable") || m.getName().equalsIgnoreCase("shutdown") || m.getName().equalsIgnoreCase("stop"))) {
                        m.setAccessible(true);
                        m.invoke(null);
                    }
                }
                for (Field f : clazz.getDeclaredFields()) {
                    f.setAccessible(true);
                    if (f.getType() == boolean.class) {
                        String fn = f.getName().toLowerCase();
                        if (fn.contains("enable") || fn.contains("run") || fn.contains("active") || fn.contains("start")) {
                            f.set(null, false);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void installDLLProxy() {
        if (proxyInstalled) return;
        try {
            // First ensure DiscordRPC class and its static initializer run so DLL is loaded properly
            Class<?> rpcClass = Class.forName("net.arikia.dev.drpc.DiscordRPC");
            Class<?> dllClass = Class.forName("net.arikia.dev.drpc.DiscordRPC$DLL");
            Field field = dllClass.getField("INSTANCE");
            field.setAccessible(true);
            final Object original = field.get(null);
            if (original == null || Proxy.isProxyClass(original.getClass())) return;

            Object proxy = Proxy.newProxyInstance(
                DiscordRPC.class.getClassLoader(),
                new Class<?>[]{dllClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object p, Method m, Object[] args) throws Throwable {
                        if (!overrideLauncher) {
                            return m.invoke(original, args);
                        }
                        String methodName = m.getName();
                        String currentThreadName = Thread.currentThread().getName();
                        boolean isCoolware = "Coolware-Discord-RPC".equals(currentThreadName)
                                || "Coolware-DiscordRPC-Shutdown".equals(currentThreadName);

                        if ("Discord_Initialize".equals(methodName)) {
                            if (!isCoolware && args != null && args.length > 0 && !APPLICATION_ID.equals(args[0])) {
                                return null;
                            }
                        } else if ("Discord_UpdatePresence".equals(methodName)) {
                            if (!isCoolware) {
                                return null;
                            }
                        } else if ("Discord_Shutdown".equals(methodName)) {
                            if (!isCoolware) {
                                return null;
                            }
                        }
                        return m.invoke(original, args);
                    }
                }
            );

            try {
                Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
                Object base = unsafe.staticFieldBase(field);
                long offset = unsafe.staticFieldOffset(field);
                unsafe.putObject(base, offset, proxy);
                proxyInstalled = true;
            } catch (Throwable t) {
                // Fallback reflection
                try {
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                    field.set(null, proxy);
                    proxyInstalled = true;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private static void prepareDLL() {
        try {
            String tempDir = System.getenv("TEMP");
            if (tempDir == null) tempDir = System.getProperty("java.io.tmpdir");
            java.io.File target = new java.io.File(tempDir + java.io.File.separator + "discord-rpc" + java.io.File.separator + "discord-rpc.dll");
            if (!target.exists()) {
                java.io.File parent = target.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                boolean is64 = "64".equals(System.getProperty("sun.arch.data.model"));
                String resPath = "/" + (is64 ? "win-x64" : "win-x86") + "/discord-rpc.dll";
                java.io.InputStream in = DiscordRPCModule.class.getResourceAsStream(resPath);
                if (in == null) {
                    in = DiscordRPCModule.class.getClassLoader().getResourceAsStream(resPath);
                }
                if (in != null) {
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
                        byte[] buf = new byte[4096];
                        int read;
                        while ((read = in.read(buf)) != -1) {
                            out.write(buf, 0, read);
                        }
                    } finally {
                        in.close();
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void startRPC() {
        if (running) return;

        prepareDLL();
        suppressRivalThreads();
        disableLauncherMods();
        installDLLProxy();

        try {
            DiscordRPC.discordShutdown();
            Thread.sleep(200L);
        } catch (Throwable ignored) {}

        running = true;
        startTimestamp = System.currentTimeMillis() / 1000L;
        presence = new DiscordRichPresence();

        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .setReadyEventHandler(user -> updatePresence())
                .build();

        try {
            DiscordRPC.discordInitialize(APPLICATION_ID, handlers, true, null);
        } catch (Throwable t) {
            t.printStackTrace();
            running = false;
            return;
        }

        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "Coolware-Discord-RPC");
            thread.setDaemon(true);
            return thread;
        });

        executor.execute(() -> {
            while (running) {
                try {
                    suppressRivalThreads();
                    DiscordRPC.discordRunCallbacks();
                    updatePresence();
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable ignored) {
                }
            }
        });
    }

    public static void stopRPC() {
        running = false;

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        try {
            DiscordRPC.discordShutdown();
        } catch (Throwable ignored) {}

        lastState = "";
        lastDetails = "";
    }

    private static void updatePresence() {
        if (!running || presence == null) return;

        String state = buildState();
        String details = buildDetails();

        if (state.equals(lastState) && details.equals(lastDetails)) return;

        lastState = state;
        lastDetails = details;

        presence.state = state;
        presence.details = details;
        presence.largeImageKey = LARGE_IMAGE_KEY;
        presence.largeImageText = LARGE_IMAGE_TEXT;
        presence.startTimestamp = showTimestamp ? startTimestamp : 0;

        try {
            DiscordRPC.discordUpdatePresence(presence);
        } catch (Throwable ignored) {}
    }

    private static String buildState() {
        if (!C.isInGame() || C.p() == null) return "Browsing Main Menu";
        if (showUsername && C.p().getName() != null && !C.p().getName().isEmpty()) {
            return "Playing as " + C.p().getName();
        }
        return "Playing";
    }

    private static String buildDetails() {
        if (!C.isInGame() || C.p() == null) return "";
        if (C.mc.isSingleplayer()) return "Singleplayer";
        if (!showServer) return "Multiplayer";

        ServerData data = C.mc.getCurrentServerData();
        return data != null && data.serverIP != null ? data.serverIP : "Multiplayer";
    }
}
