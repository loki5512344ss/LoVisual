package dev.loki.lovisual.util.proxy.model;

import com.google.gson.annotations.SerializedName;

public enum ProxyType {
    @SerializedName("SOCKS4")
    SOCKS4,

    @SerializedName("SOCKS5")
    SOCKS5;

    public static ProxyType safe(ProxyType type) {
        return type == null ? SOCKS5 : type;
    }
}
