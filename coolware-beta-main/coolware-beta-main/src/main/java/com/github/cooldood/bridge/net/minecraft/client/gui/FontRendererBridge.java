package com.github.cooldood.bridge.net.minecraft.client.gui;

public interface FontRendererBridge {
    static FontRendererBridge from(Object instance) {
        return (FontRendererBridge) instance;
    }

    void bridge$resetStyles();
    int bridge$renderString(String text, float x, float y, int color, boolean dropShadow);
    float bridge$renderChar(char ch, boolean italic);
    int bridge$renderDirect(CharSequence text, float x, float y, int color, boolean dropShadow);
}
