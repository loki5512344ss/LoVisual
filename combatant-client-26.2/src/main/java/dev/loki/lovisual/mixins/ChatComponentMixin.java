/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ChatComponent.DisplayMode;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.command.CommandOutput;
import dev.loki.lovisual.features.gui.chat.BetterChatStoreManager;
import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.misc.MessageFilter;
import dev.loki.lovisual.features.module.modules.misc.NameProtect;
import dev.loki.lovisual.features.module.modules.misc.StreamerMode;
import dev.loki.lovisual.Lifecycle;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component lovisual$nameProtectChat(Component message) {
        if (!Lifecycle.canRunHud()) return message;
        Component protectedMessage = NameProtect.protect(message);
        return StreamerMode.protectComponent(protectedMessage);
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lovisual$filterChatFull(Component message,
                                          MessageSignature signatureData,
                                          GuiMessageSource source,
                                          GuiMessageTag indicator,
                                          CallbackInfo ci) {
        if (!Lifecycle.canRunHud()) return;
        MessageFilter messageFilter = Modules.get(MessageFilter.class);
        if (messageFilter != null && messageFilter.shouldHideChat(message)) {
            ci.cancel();
            return;
        }
        if (!CommandOutput.isLoVisualMessage(message)) {
            BetterChatStoreManager.addMessage(message);
        }
    }

    @Inject(method = "logChatMessage(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$commands$suppressClientLog(GuiMessage message, CallbackInfo ci) {
        if (message != null && CommandOutput.isLoVisualMessage(message.content())) {
            ci.cancel();
        }
    }

    @Inject(method = "clearMessages(Z)V", at = @At("TAIL"))
    private void lovisual$betterChat$clear(boolean clearHistory, CallbackInfo ci) {
        if (!Lifecycle.canRunHud()) return;
        if (!BetterChat.isActive()) return;
        // Never wipe BetterChat store on vanilla clear; just persist to disk.
        BetterChatStoreManager.flushActive();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void lovisual$betterChat$hideVanilla(GuiGraphicsExtractor context,
                                                  Font textRenderer,
                                                  int currentTick,
                                                  int mouseX,
                                                  int mouseY,
                                                  DisplayMode displayMode,
                                                  boolean inChat,
                                                  CallbackInfo ci) {
        if (!Lifecycle.canRunHud()) return;
        if (!BetterChat.isActive()) return;
        BetterChat cfg = BetterChat.get();
        if (cfg != null && cfg.hideVanilla()) {
            ci.cancel();
        }
    }
}

