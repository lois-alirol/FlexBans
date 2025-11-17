package fr.neocle.flexbans.bukkit.commands;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class TestCommand implements CommandExecutor, Listener {

    private static final Key BAN_KEY = Key.key("flexbans:ban_player");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(
                        DialogBase.builder(Component.text("Advanced Ban System", NamedTextColor.RED, TextDecoration.BOLD))
                                .body(List.of(
                                        DialogBody.plainMessage(
                                                Component.text("Configure ban parameters", NamedTextColor.GRAY)
                                        ),
                                        DialogBody.plainMessage(
                                                Component.text("⚠ This action is permanent and will be logged", NamedTextColor.GOLD)
                                                        .decorate(TextDecoration.ITALIC)
                                        )
                                ))
                                .inputs(List.of(
                                        DialogInput.text(
                                                        "target",
                                                        Component.text("Target Username", NamedTextColor.YELLOW)
                                                )
                                                .width(300)
                                                .maxLength(16)
                                                .build(),

                                        DialogInput.text(
                                                        "reason",
                                                        Component.text("Ban Reason", NamedTextColor.YELLOW)
                                                )
                                                .width(300)
                                                .maxLength(256)
                                                .build(),

                                        DialogInput.numberRange(
                                                        "duration",
                                                        Component.text("Duration (days)", NamedTextColor.AQUA),
                                                        0f,
                                                        365f
                                                )
                                                .step(1f)
                                                .initial(7f)
                                                .labelFormat("%s: %s days (0 = Permanent)")
                                                .width(300)
                                                .build(),

                                        DialogInput.numberRange(
                                                        "scope",
                                                        Component.text("Ban Scope", NamedTextColor.LIGHT_PURPLE),
                                                        1f,
                                                        3f
                                                )
                                                .step(1f)
                                                .initial(2f)
                                                .labelFormat("%s: Level %s (1=Server, 2=Network, 3=Global)")
                                                .width(300)
                                                .build(),

                                        DialogInput.bool(
                                                        "ipScope",
                                                        Component.text("Enable IP Ban", NamedTextColor.DARK_RED)
                                                )
                                                .initial(false)
                                                .onTrue("Enabled")
                                                .onFalse("Disabled")
                                                .build(),

                                        DialogInput.bool(
                                                        "silent",
                                                        Component.text("Silent Mode", NamedTextColor.GREEN)
                                                )
                                                .initial(false)
                                                .onTrue("Silent")
                                                .onFalse("Public")
                                                .build()
                                ))
                                .build()
                )
                .type(
                        DialogType.confirmation(
                                ActionButton.builder(Component.text("✓ Execute Ban", NamedTextColor.RED, TextDecoration.BOLD))
                                        .tooltip(Component.text("Confirm and execute the ban", NamedTextColor.GRAY))
                                        .width(150)
                                        .action(DialogAction.customClick(BAN_KEY, null))
                                        .build(),
                                ActionButton.builder(Component.text("✗ Cancel", NamedTextColor.WHITE))
                                        .tooltip(Component.text("Cancel the operation", NamedTextColor.GRAY))
                                        .width(150)
                                        .build()
                        )
                )
        );

        player.showDialog(dialog);
        return true;
    }

    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        if (!event.getIdentifier().equals(BAN_KEY)) return;

        var response = event.getDialogResponseView();
        if (response == null) return;

        String targetName = response.getText("target");
        String reason = response.getText("reason");
        Float duration = response.getFloat("duration");
        Float scope = response.getFloat("scope");
        Boolean ipScope = response.getBoolean("ipScope");
        Boolean silent = response.getBoolean("silent");

        if (targetName == null || targetName.isEmpty() || reason == null || reason.isEmpty()) {
            return;
        }

        var target = Bukkit.getPlayer(targetName);
        boolean isSilent = silent != null && silent;
        boolean isIpBan = ipScope != null && ipScope;
        int banScope = scope != null ? scope.intValue() : 2;
        int banDuration = duration != null ? duration.intValue() : 7;

        if (target != null) {
            Component kickMessage = Component.text()
                    .append(Component.text("You have been banned", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Reason: ", NamedTextColor.GRAY))
                    .append(Component.text(reason, NamedTextColor.WHITE))
                    .append(Component.newline())
                    .append(Component.text("Duration: ", NamedTextColor.GRAY))
                    .append(Component.text(banDuration == 0 ? "Permanent" : banDuration + " days", NamedTextColor.WHITE))
                    .append(Component.newline())
                    .append(Component.text("Type: ", NamedTextColor.GRAY))
                    .append(Component.text(isIpBan ? "IP Ban" : "Account Ban", NamedTextColor.WHITE))
                    .build();

            target.kick(kickMessage);

            if (!isSilent) {
                Bukkit.broadcast(Component.text()
                        .append(Component.text("⚠ ", NamedTextColor.GOLD))
                        .append(Component.text(targetName, NamedTextColor.YELLOW))
                        .append(Component.text(" has been ", NamedTextColor.GRAY))
                        .append(Component.text(isIpBan ? "IP banned" : "banned", NamedTextColor.RED))
                        .append(Component.text(": ", NamedTextColor.GRAY))
                        .append(Component.text(reason, NamedTextColor.WHITE))
                        .build()
                );
            }
        }
    }
}