package dev.loki.lovisual.util.session.microsoft.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.loki.lovisual.util.session.MicrosoftSessionResult;
import dev.loki.lovisual.util.session.microsoft.MicrosoftJson;
import dev.loki.lovisual.util.session.microsoft.model.MicrosoftTokens;
import dev.loki.lovisual.util.session.microsoft.model.MinecraftProfile;
import dev.loki.lovisual.util.session.microsoft.model.XboxToken;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Xbox Live → XSTS → Minecraft access → profile token-exchange chain for
 * {@link MicrosoftAuthService}. Given Microsoft OAuth tokens, walks the four-endpoint pipeline and
 * assembles the resulting {@link MicrosoftSessionResult}.
 */
final class MicrosoftXboxFlow {
    private MicrosoftXboxFlow() {
    }

    static CompletableFuture<MicrosoftSessionResult> login(MicrosoftTokens tokens) {
        if (tokens == null) {
            return MicrosoftHttp.failedLoggedFuture("build Microsoft session", new NullPointerException("tokens"));
        }

        return microsoftAccessToXbox(tokens.accessToken())
                .thenCompose(xbl -> xboxToXsts(xbl.token(), xbl.userHash()))
                .thenCompose(xsts -> xstsToMinecraftAccess(xsts.token(), xsts.userHash()))
                .thenCompose(minecraftAccess -> minecraftAccessToProfile(minecraftAccess)
                        .thenApply(profile -> new MicrosoftSessionResult(profile.name(), profile.uuid(), minecraftAccess, tokens.refreshToken())));
    }

    private static CompletableFuture<XboxToken> microsoftAccessToXbox(String microsoftAccess) {
        String clean;
        try {
            clean = MicrosoftHttp.cleanToken(microsoftAccess, "microsoftAccess");
        } catch (Throwable t) {
            return MicrosoftHttp.failedLoggedFuture("exchange Microsoft access token to Xbox Live token", t);
        }

        JsonObject request = new JsonObject();
        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "d=" + clean);
        request.add("Properties", properties);
        request.addProperty("RelyingParty", "http://auth.xboxlive.com");
        request.addProperty("TokenType", "JWT");
        return MicrosoftHttp.logFailures("exchange Microsoft access token to Xbox Live token",
                MicrosoftHttp.sendJson(MicrosoftHttp.jsonPost("https://user.auth.xboxlive.com/user/authenticate", request), "exchange Microsoft access token to Xbox Live token")
                        .thenApply(XboxToken::fromJson));
    }

    private static CompletableFuture<XboxToken> xboxToXsts(String xboxToken, String expectedHash) {
        String cleanToken;
        try {
            cleanToken = MicrosoftHttp.cleanToken(xboxToken, "xboxToken");
        } catch (Throwable t) {
            return MicrosoftHttp.failedLoggedFuture("exchange Xbox Live token to XSTS", t);
        }

        JsonObject request = new JsonObject();
        JsonObject properties = new JsonObject();
        JsonArray userTokens = new JsonArray();
        userTokens.add(cleanToken);
        properties.add("UserTokens", userTokens);
        properties.addProperty("SandboxId", "RETAIL");
        request.add("Properties", properties);
        request.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        request.addProperty("TokenType", "JWT");
        return MicrosoftHttp.logFailures("exchange Xbox Live token to XSTS",
                MicrosoftHttp.sendRaw(MicrosoftHttp.jsonPost("https://xsts.auth.xboxlive.com/xsts/authorize", request), "exchange Xbox Live token to XSTS")
                        .thenApply(response -> {
                    int status = response.statusCode();
                    if (status == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        throw MicrosoftHttp.xstsUnauthorized(response);
                    }
                    if (status != HttpURLConnection.HTTP_OK) {
                        throw MicrosoftHttp.badStatus("exchange Xbox Live token to XSTS", response);
                    }
                    XboxToken token = XboxToken.fromJson(MicrosoftHttp.parseJson(response.body(), "parse XSTS response"));
                    if (expectedHash != null && !expectedHash.isBlank() && !expectedHash.equals(token.userHash())) {
                        throw new MicrosoftAuthException("Mismatching Xbox user hash between XBL and XSTS");
                    }
                    return token;
                }));
    }

    private static CompletableFuture<String> xstsToMinecraftAccess(String xstsToken, String userHash) {
        String cleanToken;
        String cleanHash;
        try {
            cleanToken = MicrosoftHttp.cleanToken(xstsToken, "xstsToken");
            cleanHash = MicrosoftHttp.cleanToken(userHash, "userHash");
        } catch (Throwable t) {
            return MicrosoftHttp.failedLoggedFuture("exchange XSTS token to Minecraft access token", t);
        }

        JsonObject request = new JsonObject();
        request.addProperty("identityToken", "XBL3.0 x=" + cleanHash + ";" + cleanToken);
        return MicrosoftHttp.logFailures("exchange XSTS token to Minecraft access token",
                MicrosoftHttp.sendJson(MicrosoftHttp.jsonPost("https://api.minecraftservices.com/authentication/login_with_xbox", request), "exchange XSTS token to Minecraft access token")
                        .thenApply(json -> MicrosoftJson.string(json, "access_token")));
    }

    private static CompletableFuture<MinecraftProfile> minecraftAccessToProfile(String minecraftAccess) {
        String clean;
        try {
            clean = MicrosoftHttp.cleanToken(minecraftAccess, "minecraftAccess");
        } catch (Throwable t) {
            return MicrosoftHttp.failedLoggedFuture("fetch Minecraft profile", t);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("User-Agent", MicrosoftHttp.USER_AGENT)
                .header("Authorization", "Bearer " + clean)
                .timeout(MicrosoftHttp.TIMEOUT)
                .GET()
                .build();
        return MicrosoftHttp.logFailures("fetch Minecraft profile",
                MicrosoftHttp.sendRaw(request, "fetch Minecraft profile")
                        .thenApply(response -> {
                    int status = response.statusCode();
                    if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                        throw new MicrosoftAuthException("Microsoft account has no Minecraft profile", "lovisual.auth.microsoft.no_profile");
                    }
                    if (status != HttpURLConnection.HTTP_OK) {
                        throw MicrosoftHttp.badStatus("fetch Minecraft profile", response);
                    }
                    return MinecraftProfile.fromJson(MicrosoftHttp.parseJson(response.body(), "parse Minecraft profile"));
                }));
    }
}
