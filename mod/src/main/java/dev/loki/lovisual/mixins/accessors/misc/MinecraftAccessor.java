package dev.loki.lovisual.mixins.accessors.misc;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.FriendsService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.server.Services;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    @Accessor("rightClickDelay")
    int lovisual$getItemUseCooldown();

    @Accessor("rightClickDelay")
    void lovisual$setItemUseCooldown(int itemUseCooldown);

    @Mutable
    @Accessor("user")
    void lovisual$setSession(User session);

    @Mutable
    @Accessor("services")
    void lovisual$setServices(Services services);

    @Mutable
    @Accessor("profileFuture")
    void lovisual$setProfileFuture(CompletableFuture<ProfileResult> profileFuture);

    @Mutable
    @Accessor("userApiService")
    void lovisual$setUserApiService(UserApiService userApiService);

    @Mutable
    @Accessor("userPropertiesFuture")
    void lovisual$setUserPropertiesFuture(CompletableFuture<UserApiService.UserProperties> userPropertiesFuture);

    @Mutable
    @Accessor("playerSocialManager")
    void lovisual$setPlayerSocialManager(PlayerSocialManager playerSocialManager);

    @Accessor("remoteFriendListUpdateHandler")
    RemoteFriendListUpdateHandler lovisual$getRemoteFriendListUpdateHandler();

    @Mutable
    @Accessor("remoteFriendListUpdateHandler")
    void lovisual$setRemoteFriendListUpdateHandler(RemoteFriendListUpdateHandler remoteFriendListUpdateHandler);

    @Mutable
    @Accessor("telemetryManager")
    void lovisual$setTelemetryManager(ClientTelemetryManager telemetryManager);

    @Mutable
    @Accessor("profileKeyPairManager")
    void lovisual$setProfileKeyPairManager(ProfileKeyPairManager profileKeyPairManager);

    @Mutable
    @Accessor("reportingContext")
    void lovisual$setReportingContext(ReportingContext reportingContext);

    @Invoker("createUserApiService")
    static UserApiService lovisual$createUserApiService(YggdrasilAuthenticationService service, GameConfig config) {
        throw new AssertionError();
    }
}
