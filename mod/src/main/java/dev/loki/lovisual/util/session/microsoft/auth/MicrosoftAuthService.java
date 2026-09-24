package dev.loki.lovisual.util.session.microsoft.auth;

import dev.loki.lovisual.util.session.MicrosoftSessionResult;
import dev.loki.lovisual.util.session.microsoft.model.MicrosoftTokens;

import java.net.http.HttpRequest;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Microsoft OAuth / Xbox Live / Minecraft authentication entry points.
 *
 * <p>Facade holding the public login flows; HTTP plumbing lives in {@link MicrosoftHttp}, the
 * device-code polling loop in {@link MicrosoftDeviceFlow} and the Xbox/XSTS/Minecraft exchange
 * chain in {@link MicrosoftXboxFlow}.
 */
public final class MicrosoftAuthService {

    private MicrosoftAuthService() {
    }

    public static CompletableFuture<MicrosoftDeviceCode> requestDeviceCode() {
        String payload = "client_id=" + MicrosoftHttp.CLIENT_ID + "&scope=XboxLive.signin%20XboxLive.offline_access";
        HttpRequest request = MicrosoftHttp.formPost("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode", payload);
        return MicrosoftHttp.logFailures("request Microsoft device code",
                MicrosoftHttp.sendJson(request, "request Microsoft device code")
                        .thenApply(MicrosoftDeviceCode::fromJson));
    }

    public static CompletableFuture<MicrosoftSessionResult> loginWithDeviceCode(MicrosoftDeviceCode code) {
        if (code == null) {
            return MicrosoftHttp.failedLoggedFuture("start Microsoft device-code login", new NullPointerException("code"));
        }

        Instant deadline = Instant.now().plus(code.expiresIn());
        return MicrosoftHttp.logFailures("poll Microsoft device-code login",
                CompletableFuture.supplyAsync(() -> MicrosoftDeviceFlow.pollUntilAuthorized(code, deadline), MicrosoftHttp.EXECUTOR))
                .thenCompose(MicrosoftXboxFlow::login);
    }

    public static CompletableFuture<MicrosoftSessionResult> loginWithRefreshToken(String refreshToken) {
        String clean;
        try {
            clean = MicrosoftHttp.cleanToken(refreshToken, "refreshToken");
        } catch (Throwable t) {
            return MicrosoftHttp.failedLoggedFuture("start Microsoft refresh-token login", t);
        }
        return refreshToMicrosoftTokens(clean).thenCompose(MicrosoftXboxFlow::login);
    }

    public static CompletableFuture<MicrosoftSessionResult> loginWithAuthorizationCode(String code, String redirectUri) {
        String cleanCode;
        String cleanRedirect;
        try {
            cleanCode = MicrosoftHttp.cleanToken(code, "code");
            cleanRedirect = MicrosoftHttp.cleanToken(redirectUri, "redirectUri");
        } catch (Throwable t) {
            return MicrosoftHttp.failedLoggedFuture("start Microsoft authorization-code login", t);
        }

        String payload = "client_id=" + MicrosoftHttp.CLIENT_ID +
                "&code=" + MicrosoftHttp.encode(cleanCode) +
                "&grant_type=authorization_code" +
                "&redirect_uri=" + MicrosoftHttp.encode(cleanRedirect) +
                "&scope=XboxLive.signin%20XboxLive.offline_access";
        return MicrosoftHttp.logFailures("exchange Microsoft authorization code",
                MicrosoftHttp.sendJson(MicrosoftHttp.formPost("https://login.live.com/oauth20_token.srf", payload), "exchange Microsoft authorization code")
                        .thenApply(MicrosoftTokens::fromJson))
                .thenCompose(MicrosoftXboxFlow::login);
    }

    private static CompletableFuture<MicrosoftTokens> refreshToMicrosoftTokens(String refreshToken) {
        String clean;
        try {
            clean = MicrosoftHttp.cleanToken(refreshToken, "refreshToken");
        } catch (Throwable t) {
            return MicrosoftHttp.failedLoggedFuture("refresh Microsoft OAuth token", t);
        }

        String payload = "client_id=" + MicrosoftHttp.CLIENT_ID +
                "&refresh_token=" + MicrosoftHttp.encode(clean) +
                "&grant_type=refresh_token" +
                "&scope=XboxLive.signin%20XboxLive.offline_access";
        return MicrosoftHttp.logFailures("refresh Microsoft OAuth token",
                MicrosoftHttp.sendJson(MicrosoftHttp.formPost("https://login.live.com/oauth20_token.srf", payload), "refresh Microsoft OAuth token")
                        .thenApply(MicrosoftTokens::fromJson));
    }

    static CompletableFuture<MicrosoftSessionResult> loginWithMicrosoftTokens(MicrosoftTokens tokens) {
        return MicrosoftXboxFlow.login(tokens);
    }
}
