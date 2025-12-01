package fr.neocle.flexbans.bukkit.dialogs;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.BooleanDialogInput;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

public class KickDialog {

    private static final Key DIALOG_KEY = Key.key("flexbans", "kick_dialog");
    private static final Key EXECUTE_ACTION_KEY = Key.key("flexbans", "kick_execute");

    private static final String KEY_TARGET_NAME = "kick_target_name";
    private static final String KEY_REASON = "kick_reason";

    private static final String KEY_SILENT = "kick_silent";
    private static final String KEY_KICK_TYPE = "kick_type";

    public static Dialog createKickDialog() {
        MiniMessage miniMessage = MiniMessage.miniMessage();

        TextDialogInput targetNameInput = DialogInput.text(KEY_TARGET_NAME, Component.text("Target Name / UUID"))
                .width(256)
                .initial("")
                .build();

        TextDialogInput reasonInput = DialogInput.text(KEY_REASON, Component.text("Reason"))
                .width(256)
                .maxLength(200)
                .initial("")
                .multiline(TextDialogInput.MultilineOptions.create(5, 53))
                .build();

        BooleanDialogInput silentInput = DialogInput.bool(
                        KEY_SILENT,
                        paddedLabel("Silent Kick", 37)
                )
                .initial(false)
                .onTrue("Yes")
                .onFalse("No")
                .build();

        List<DialogInput> inputs = List.of(
                targetNameInput,
                reasonInput,
                silentInput
        );

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(
                        miniMessage.deserialize("<bold><gradient:#ffa600:#ffd04e>FlexBans</gradient></bold>")
                ),
                DialogBody.plainMessage(
                        miniMessage.deserialize("<gray>Fill fields below to execute a new kick!")
                )
        );

        ActionButton executeButton = ActionButton.builder(Component.text("Execute").color(NamedTextColor.GREEN))
                .tooltip(Component.text("Apply the kick"))
                .action(DialogAction.customClick(EXECUTE_ACTION_KEY, null))
                .build();

        ActionButton cancelButton = ActionButton.builder(Component.text("Cancel").color(NamedTextColor.RED)).build();

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Kick Player").color(NamedTextColor.GOLD))
                        .body(body)
                        .inputs(inputs)
                        .build()
                )
                .type(DialogType.confirmation(executeButton, cancelButton))
        );
    }

    private static Component paddedLabel(String label, int padding) {
        return Component.text(label + " ".repeat(padding));
    }

    public static void showDialog(Player player) {
        player.showDialog(createKickDialog());
    }
}