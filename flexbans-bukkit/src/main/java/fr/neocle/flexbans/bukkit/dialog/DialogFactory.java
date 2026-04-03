package fr.neocle.flexbans.bukkit.dialog;

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

import java.util.ArrayList;
import java.util.List;

public class DialogFactory {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static Dialog createPunishDialog(String type, Key actionKey) {
        List<DialogInput> inputs = new ArrayList<>(getBaseInputs());

        inputs.add(DialogInput.numberRange("duration_value", Component.text("Duration"), 1, 500).width(256).initial(30F).build());
        inputs.add(DialogInput.singleOption("duration_unit", Component.text("Unit"), List.of(
                SingleOptionDialogInput.OptionEntry.create("days", Component.text("Days"), true),
                SingleOptionDialogInput.OptionEntry.create("hours", Component.text("Hours"), false)
        )).width(256).build());

        inputs.add(DialogInput.bool("permanent", paddedLabel("Permanent " + type, 30)).onTrue("Yes").onFalse("No").build());
        inputs.add(DialogInput.bool("silent", paddedLabel("Silent " + type, 35)).onTrue("Yes").onFalse("No").build());

        inputs.add(DialogInput.singleOption("server_scope", Component.text("Scope"), List.of(
                SingleOptionDialogInput.OptionEntry.create("global", Component.text("Global"), true),
                SingleOptionDialogInput.OptionEntry.create("local", Component.text("Local"), false)
        )).width(256).build());

        inputs.add(DialogInput.singleOption("ip_scope", Component.text("IP Scope"), List.of(
                SingleOptionDialogInput.OptionEntry.create("account", Component.text("Account"), true),
                SingleOptionDialogInput.OptionEntry.create("ip", Component.text("Account + IP"), false)
        )).width(256).build());

        return build(type, inputs, actionKey);
    }

    public static Dialog createKickDialog() {
        List<DialogInput> inputs = new ArrayList<>(getBaseInputs());
        inputs.add(DialogInput.bool("silent", paddedLabel("Silent Kick", 35)).build());
        return build("Kick", inputs, DialogKeys.KICK_EXECUTE);
    }

    public static Dialog createWarningDialog() {
        List<DialogInput> inputs = new ArrayList<>(getBaseInputs());

        inputs.add(DialogInput.bool("silent", paddedLabel("Silent Warning", 35))
                .onTrue("Yes")
                .onFalse("No")
                .build());

        inputs.add(DialogInput.singleOption("server_scope", Component.text("Scope"), List.of(
                SingleOptionDialogInput.OptionEntry.create("global", Component.text("Global"), true),
                SingleOptionDialogInput.OptionEntry.create("local", Component.text("Local"), false)
        )).width(256).build());

        inputs.add(DialogInput.singleOption("ip_scope", Component.text("IP Scope"), List.of(
                SingleOptionDialogInput.OptionEntry.create("account", Component.text("Account"), true),
                SingleOptionDialogInput.OptionEntry.create("ip", Component.text("Account + IP"), false)
        )).width(256).build());

        return build("Warning", inputs, DialogKeys.WARNING_EXECUTE);
    }

    private static List<DialogInput> getBaseInputs() {
        return List.of(
                DialogInput.text("target_name", Component.text("Player Name")).width(256).build(),
                DialogInput.text("reason", Component.text("Reason")).width(256).multiline(TextDialogInput.MultilineOptions.create(3, 50))
                        .maxLength(200)
                        .build()
        );
    }

    private static Dialog build(String title, List<DialogInput> inputs, Key actionKey) {
        ActionButton exec = ActionButton.builder(Component.text("Execute").color(NamedTextColor.GREEN))
                .action(DialogAction.customClick(actionKey, null)).build();
        ActionButton cancel = ActionButton.builder(Component.text("Cancel").color(NamedTextColor.RED)).build();

        return Dialog.create(b -> b.empty().base(DialogBase.builder(Component.text(title + " Player").color(NamedTextColor.GOLD))
                .body(List.of(DialogBody.plainMessage(MM.deserialize("<bold><gradient:#ffa600:#ffd04e>FlexBans</gradient></bold>"))))
                .inputs(inputs).build()).type(DialogType.confirmation(exec, cancel)));
    }

    private static Component paddedLabel(String label, int padding) {
        return Component.text(label + " ".repeat(padding));
    }
}