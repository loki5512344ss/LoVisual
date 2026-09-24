package dev.loki.lovisual.util.session.microsoft.auth;

import com.google.gson.JsonObject;
import dev.loki.lovisual.util.session.microsoft.MicrosoftJson;
import dev.loki.lovisual.util.session.microsoft.model.MicrosoftTokens;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.time.Instant;

/**
 * Device-code polling loop for {@link MicrosoftAuthService}: repeatedly exchanges the device code
 * for Microsoft OAuth tokens until the user authorizes, the flow is declined or the deadline passes.
 */
final class MicrosoftDeviceFlow {
    private MicrosoftDeviceFlow() {
    }

    static MicrosoftTokens pollUntilAuthorized(MicrosoftDeviceCode code, Instant deadline) {
        DeviceAuthPendingException pending = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                return deviceCodeToMicrosoftTokens(code.deviceCode());
            } catch (DeviceAuthPendingException e) {
                pending = e;
                try {
                    Thread.sleep(Math.max(1000L, code.interval().toMillis()));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new MicrosoftAuthException("Microsoft device-code login interrupted", interrupted);
                }
            }
        }
        throw new MicrosoftAuthException("Microsoft device-code login expired", pending, "lovisual.auth.microsoft.expired");
    }

    private static MicrosoftTokens deviceCodeToMicrosoftTokens(String deviceCode) {
        String clean = MicrosoftHttp.cleanToken(deviceCode, "deviceCode");
        String payload = "grant_type=urn:ietf:params:oauth:grant-type:device_code" +
                "&client_id=" + MicrosoftHttp.CLIENT_ID +
                "&device_code=" + MicrosoftHttp.encode(clean);
        HttpResponse<String> response;
        try {
            response = MicrosoftHttp.CLIENT.send(
                    MicrosoftHttp.formPost("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", payload),
                    HttpResponse.BodyHandlers.ofString());
        } catch (Throwable t) {
            throw new MicrosoftAuthException("Unable to poll Microsoft device-code login", t);
        }
        int status = response.statusCode();
        if (status == HttpURLConnection.HTTP_OK) {
            return MicrosoftTokens.fromJson(MicrosoftHttp.parseJson(response.body(), "parse Microsoft device-code token response"));
        }
        try {
            JsonObject json = MicrosoftHttp.parseJson(response.body(), "parse Microsoft device-code error");
            String error = MicrosoftJson.string(json, "error");
            if ("authorization_pending".equals(error)) {
                throw new DeviceAuthPendingException("Microsoft device-code login is still pending");
            }
            if ("authorization_declined".equals(error)) {
                throw new MicrosoftAuthException("Microsoft device-code login was cancelled by the user", "lovisual.auth.microsoft.cancelled");
            }
            if ("expired_token".equals(error)) {
                throw new MicrosoftAuthException("Microsoft device-code login expired", "lovisual.auth.microsoft.expired");
            }
        } catch (DeviceAuthPendingException | MicrosoftAuthException e) {
            throw e;
        } catch (Throwable ignored) {
        }
        throw MicrosoftHttp.badStatus("poll Microsoft device-code login", response);
    }
}
