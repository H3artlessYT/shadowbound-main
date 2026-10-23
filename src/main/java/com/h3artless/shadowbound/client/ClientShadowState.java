package com.h3artless.shadowbound.client;

public final class ClientShadowState {
    private static int darkness;

    private ClientShadowState() {}

    public static int darkness() {
        return darkness;
    }

    public static void setDarkness(int value) {
        darkness = value;
    }
}
