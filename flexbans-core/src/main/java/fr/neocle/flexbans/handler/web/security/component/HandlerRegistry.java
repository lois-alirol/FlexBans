package fr.neocle.flexbans.handler.web.security.component;

import fr.neocle.flexbans.handler.web.*;
import fr.neocle.flexbans.handler.web.api.PunishmentSSEHandler;
import fr.neocle.flexbans.handler.web.post.NewPunishmentHandler;
import fr.neocle.flexbans.handler.web.post.RevokePunishmentHandler;

public class HandlerRegistry {
    private final IndexHandler indexHandler;
    private final PlayerHistoryHandler playerHistoryHandler;
    private final ModeratorHistoryHandler moderatorHistoryHandler;
    private final PunishmentDetailsHandler punishmentDetailsHandler;
    private final PlayerHeadHandler playerHeadHandler;
    private final JavascriptHandler javascriptHandler;
    private final CssHandler cssHandler;
    private final RevokePunishmentHandler revokePunishmentHandler;
    private final NewPunishmentHandler newPunishmentHandler;
    private final PunishmentSSEHandler punishmentSSEHandler;

    public HandlerRegistry(
            IndexHandler indexHandler,
            PlayerHistoryHandler playerHistoryHandler,
            ModeratorHistoryHandler moderatorHistoryHandler,
            PunishmentDetailsHandler punishmentDetailsHandler,
            PlayerHeadHandler playerHeadHandler,
            JavascriptHandler javascriptHandler,
            CssHandler cssHandler,
            RevokePunishmentHandler revokePunishmentHandler,
            NewPunishmentHandler newPunishmentHandler,
            PunishmentSSEHandler punishmentSSEHandler) {
        this.indexHandler = indexHandler;
        this.playerHistoryHandler = playerHistoryHandler;
        this.moderatorHistoryHandler = moderatorHistoryHandler;
        this.punishmentDetailsHandler = punishmentDetailsHandler;
        this.playerHeadHandler = playerHeadHandler;
        this.javascriptHandler = javascriptHandler;
        this.cssHandler = cssHandler;
        this.revokePunishmentHandler = revokePunishmentHandler;
        this.newPunishmentHandler = newPunishmentHandler;
        this.punishmentSSEHandler = punishmentSSEHandler;
    }

    public IndexHandler getIndexHandler() {
        return indexHandler;
    }

    public PlayerHistoryHandler getPlayerHistoryHandler() {
        return playerHistoryHandler;
    }

    public ModeratorHistoryHandler getModeratorHistoryHandler() {
        return moderatorHistoryHandler;
    }

    public PunishmentDetailsHandler getPunishmentDetailsHandler() {
        return punishmentDetailsHandler;
    }

    public PlayerHeadHandler getPlayerHeadHandler() {
        return playerHeadHandler;
    }

    public JavascriptHandler getJavascriptHandler() {
        return javascriptHandler;
    }

    public CssHandler getCssHandler() {
        return cssHandler;
    }

    public RevokePunishmentHandler getRevokePunishmentHandler() {
        return revokePunishmentHandler;
    }

    public NewPunishmentHandler getNewPunishmentHandler() {
        return newPunishmentHandler;
    }

    public PunishmentSSEHandler getPunishmentSSEHandler() {
        return punishmentSSEHandler;
    }
}