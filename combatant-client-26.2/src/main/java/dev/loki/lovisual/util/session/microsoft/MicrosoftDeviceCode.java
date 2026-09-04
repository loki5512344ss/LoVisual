/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.session.microsoft;

import com.google.gson.JsonObject;

import java.net.URI;
import java.time.Duration;

public record MicrosoftDeviceCode(String deviceCode,
                                  String userCode,
                                  URI verificationUri,
                                  Duration expiresIn,
                                  Duration interval) {

    static MicrosoftDeviceCode fromJson(JsonObject json) {
        try {
            String device = MicrosoftJson.string(json, "device_code");
            String user = MicrosoftJson.string(json, "user_code");
            URI uri = new URI(MicrosoftJson.string(json, "verification_uri")).parseServerAuthority();
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalStateException("Invalid verification URI scheme: " + uri);
            }
            Duration expires = Duration.ofSeconds(MicrosoftJson.number(json, "expires_in"));
            Duration poll = Duration.ofSeconds(MicrosoftJson.number(json, "interval"));
            if (expires.isZero() || expires.isNegative()) {
                throw new IllegalStateException("Invalid device-code expiration: " + expires);
            }
            if (poll.isZero() || poll.isNegative() || poll.compareTo(expires) >= 0) {
                throw new IllegalStateException("Invalid device-code polling interval: " + poll);
            }
            return new MicrosoftDeviceCode(device, user, uri, expires, poll);
        } catch (Throwable t) {
            throw new MicrosoftAuthException("Unable to parse Microsoft device-code response", t);
        }
    }
}
