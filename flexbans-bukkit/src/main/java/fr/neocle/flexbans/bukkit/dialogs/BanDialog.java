package fr.neocle.flexbans.bukkit.dialogs;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.*;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

public class BanDialog {

    private static final Key DIALOG_KEY = Key.key("flexbans", "ban_dialog");
    private static final Key EXECUTE_ACTION_KEY = Key.key("flexbans", "ban_execute");

    private static final String KEY_TARGET_NAME = "ban_target_name";
    private static final String KEY_REASON = "ban_reason";
    private static final String KEY_DURATION_VALUE = "ban_duration_value";

    private static final String KEY_DURATION_UNIT = "ban_duration_unit";

    private static final String KEY_PERMANENT = "ban_permanent";
    private static final String KEY_SILENT = "ban_silent";
    private static final String KEY_SERVER_SCOPE = "ban_server_scope";
    private static final String KEY_BAN_TYPE = "ban_type";

    public static Dialog createBanDialog() {
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

        List<SingleOptionDialogInput.OptionEntry> unitOptions = List.of(
                SingleOptionDialogInput.OptionEntry.create("seconds", Component.text("Seconds"), false),
                SingleOptionDialogInput.OptionEntry.create("minutes", Component.text("Minutes"), false),
                SingleOptionDialogInput.OptionEntry.create("hours", Component.text("Hours"), false),
                SingleOptionDialogInput.OptionEntry.create("days", Component.text("Days"), true),
                SingleOptionDialogInput.OptionEntry.create("months", Component.text("Months"), false),
                SingleOptionDialogInput.OptionEntry.create("years", Component.text("Years"), false)
        );

        SingleOptionDialogInput durationUnitInput = DialogInput.singleOption(
                        KEY_DURATION_UNIT,
                        Component.text("Duration Unit"),
                        unitOptions
                )
                .width(256)
                .build();

        NumberRangeDialogInput durationValueInput = DialogInput.numberRange(
                        KEY_DURATION_VALUE,
                        Component.text("Duration Value"),
                        1,
                        500
                )
                .width(256)
                .initial(30F)
                .step(1F)
                .build();

        BooleanDialogInput permanentInput = DialogInput.bool(
                        KEY_PERMANENT,
                        paddedLabel("Permanent Ban", 32)
                )
                .initial(false)
                .onTrue("Yes")
                .onFalse("No")
                .build();

        BooleanDialogInput silentInput = DialogInput.bool(
                        KEY_SILENT,
                        paddedLabel("Silent Ban", 37)
                )
                .initial(false)
                .onTrue("Yes")
                .onFalse("No")
                .build();

        List<SingleOptionDialogInput.OptionEntry> serverScopeOptions = List.of(
                SingleOptionDialogInput.OptionEntry.create("local", Component.text("This Server Only"), true),
                SingleOptionDialogInput.OptionEntry.create("global", Component.text("All Servers").color(NamedTextColor.RED), false)
        );

        SingleOptionDialogInput serverScopeInput = DialogInput.singleOption(
                        KEY_SERVER_SCOPE,
                        Component.text("Server Scope"),
                        serverScopeOptions
                )
                .width(256)
                .build();

        List<SingleOptionDialogInput.OptionEntry> banTypeOptions = List.of(
                SingleOptionDialogInput.OptionEntry.create("account", Component.text("Account Only"), true),
                SingleOptionDialogInput.OptionEntry.create("ip_and_account", Component.text("Account + IP").color(NamedTextColor.RED), false)
        );
        SingleOptionDialogInput banTypeInput = DialogInput.singleOption(KEY_BAN_TYPE, Component.text("Ban Type"), banTypeOptions)
                .width(256)
                .build();

        List<DialogInput> inputs = List.of(
                targetNameInput,
                reasonInput,
                durationValueInput,
                durationUnitInput,
                permanentInput,
                silentInput,
                serverScopeInput,
                banTypeInput
        );

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(
                        miniMessage.deserialize("<bold><gradient:#ffa600:#ffd04e>FlexBans</gradient></bold>")
                ),
                DialogBody.plainMessage(
                        miniMessage.deserialize("<gray>Fill fields below to execute a new ban!")
                )
        );

        ActionButton executeButton = ActionButton.builder(Component.text("Execute").color(NamedTextColor.GREEN))
                .tooltip(Component.text("Apply the ban"))
                .action(DialogAction.customClick(EXECUTE_ACTION_KEY, null))
                .build();

        ActionButton cancelButton = ActionButton.builder(Component.text("Cancel").color(NamedTextColor.RED)).build();

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Ban Player").color(NamedTextColor.GOLD))
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
        player.showDialog(createBanDialog());
    }
}