package dev.loki.lovisual.util.proxy;

import dev.loki.lovisual.util.proxy.model.ProxyEntry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Outbound SOCKS4/SOCKS5 handshake message encoders used by {@link ProxyHandshakeHandler}. These are
 * pure {@link ByteBuf} builders that read only the resolved {@link ProxyEntry} credentials and the
 * target address handed in by the handler, keeping behaviour identical to the original inlined
 * request-creation methods.
 */
final class ProxyProtocolMessages {
    private ProxyProtocolMessages() {
    }

    static ByteBuf greeting(ChannelHandlerContext ctx, ProxyEntry proxy) {
        ByteBuf out = ctx.alloc().buffer(3);
        out.writeByte(5);
        out.writeByte(1);
        out.writeByte(proxy.hasCredentials() ? 0x02 : 0x00);
        return out;
    }

    static ByteBuf authRequest(ChannelHandlerContext ctx, ProxyEntry proxy) {
        byte[] username = ascii(proxy.username());
        byte[] password = ascii(proxy.password());
        if (username.length > 255 || password.length > 255) {
            throw new IllegalArgumentException("SOCKS5 username/password is too long");
        }

        ByteBuf out = ctx.alloc().buffer(3 + username.length + password.length);
        out.writeByte(1);
        out.writeByte(username.length);
        out.writeBytes(username);
        out.writeByte(password.length);
        out.writeBytes(password);
        return out;
    }

    static ByteBuf socks5ConnectRequest(ChannelHandlerContext ctx, InetSocketAddress target) {
        ByteBuf out = ctx.alloc().buffer(32);
        out.writeByte(5);
        out.writeByte(1);
        out.writeByte(0);
        writeSocks5Address(out, target);
        out.writeShort(target.getPort());
        return out;
    }

    static ByteBuf socks4ConnectRequest(ChannelHandlerContext ctx, ProxyEntry proxy, InetSocketAddress target) {
        ByteBuf out = ctx.alloc().buffer(64);
        out.writeByte(4);
        out.writeByte(1);
        out.writeShort(target.getPort());

        InetAddress address = target.getAddress();
        boolean ipv4 = address instanceof Inet4Address;
        if (ipv4) {
            out.writeBytes(address.getAddress());
        } else {
            out.writeByte(0);
            out.writeByte(0);
            out.writeByte(0);
            out.writeByte(1);
        }

        if (proxy.hasUsername()) {
            out.writeBytes(ascii(proxy.username()));
        }
        out.writeByte(0);

        if (!ipv4) {
            out.writeBytes(ascii(target.getHostString()));
            out.writeByte(0);
        }
        return out;
    }

    private static void writeSocks5Address(ByteBuf out, InetSocketAddress address) {
        InetAddress inetAddress = address.getAddress();
        if (inetAddress instanceof Inet4Address) {
            out.writeByte(1);
            out.writeBytes(inetAddress.getAddress());
            return;
        }
        if (inetAddress instanceof Inet6Address) {
            out.writeByte(4);
            out.writeBytes(inetAddress.getAddress());
            return;
        }

        byte[] host = ascii(address.getHostString());
        if (host.length > 255) {
            throw new IllegalArgumentException("SOCKS5 host is too long: " + address.getHostString());
        }
        out.writeByte(3);
        out.writeByte(host.length);
        out.writeBytes(host);
    }

    private static byte[] ascii(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.US_ASCII);
    }
}
