package de.minecraftmodmaker.choicervoicer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.minecraftmodmaker.choicervoicer.ChoicerVoicerMod;
import de.minecraftmodmaker.choicervoicer.game.GameSession;
import de.minecraftmodmaker.choicervoicer.pack.ContentPacks;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class ChoicerCommands {
    private ChoicerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("choicervoicer")
                .then(Commands.literal("join").executes(ChoicerCommands::join))
                .then(Commands.literal("leave").executes(ChoicerCommands::leave))
                .then(Commands.literal("status").executes(ChoicerCommands::status))
                .then(Commands.literal("packs").executes(ChoicerCommands::packs))
                .then(Commands.literal("start")
                        .requires(ChoicerCommands::operator)
                        .then(Commands.argument("pack", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        mod().packs().registry().voicePacks().keySet(), builder))
                                .executes(ChoicerCommands::start)))
                .then(Commands.literal("stop")
                        .requires(ChoicerCommands::operator)
                        .executes(ChoicerCommands::stop))
                .then(Commands.literal("reload")
                        .requires(ChoicerCommands::operator)
                        .executes(ChoicerCommands::reload))
                .then(Commands.literal("import")
                        .requires(ChoicerCommands::operator)
                        .executes(ChoicerCommands::importPacks)));
    }

    private static boolean operator(CommandSourceStack source) {
        return switch (mod().config().operatorPermissionLevel()) {
            case 0 -> true;
            case 1 -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
            case 2 -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
            case 3 -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
            default -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER);
        };
    }

    private static int join(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!mod().session().join(player)) {
            context.getSource().sendFailure(Component.literal(
                    "Beitritt nicht möglich. Die Lobby muss offen und Simple Voice Chat verbunden sein."));
            return 0;
        }
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!mod().session().leave(player)) {
            context.getSource().sendFailure(Component.literal("Du nimmst an keinem Spiel teil."));
            return 0;
        }
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(mod().session().status()), false);
        return 1;
    }

    private static int packs(CommandContext<CommandSourceStack> context) {
        var registry = mod().packs().registry();
        context.getSource().sendSuccess(() -> Component.literal(
                "Voice-Packs (" + registry.voicePacks().size() + "): "
                        + String.join(", ", registry.voicePacks().keySet())), false);
        context.getSource().sendSuccess(() -> Component.literal(
                "Judge-Packs (" + registry.judgePacks().size() + "): "
                        + String.join(", ", registry.judgePacks().keySet())), false);
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "pack");
        ContentPacks.VoicePack pack = mod().packs().registry().voicePacks().get(id);
        if (pack == null) {
            context.getSource().sendFailure(Component.literal("Unbekanntes Voice-Pack: " + id));
            return 0;
        }
        ServerPlayer host = context.getSource().getPlayerOrException();
        if (!mod().session().start(pack, host)) {
            context.getSource().sendFailure(Component.literal(
                    "Spiel konnte nicht gestartet werden. Prüfe Sitzung und Voice-Chat-Verbindung."));
            return 0;
        }
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        mod().session().stop("Von einem Operator gestoppt.");
        context.getSource().sendSuccess(() -> Component.literal("Spiel gestoppt."), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        var registry = mod().packs().reload();
        context.getSource().sendSuccess(() -> Component.literal(
                "Neu geladen: " + registry.voicePacks().size() + " Voice-, "
                        + registry.judgePacks().size() + " Judge-Packs."), false);
        return 1;
    }

    private static int importPacks(CommandContext<CommandSourceStack> context) {
        ContentPacks.ImportReport report = mod().packs().importArchives();
        context.getSource().sendSuccess(() -> Component.literal(
                "Import: " + report.importedArchives() + " Archive, "
                        + report.voicePacks() + " Voice-, " + report.judgePacks() + " Judge-Packs.")
                .withStyle(report.successful() ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        report.issues().stream().limit(10).forEach(issue ->
                context.getSource().sendFailure(Component.literal(
                        issue.severity() + " " + issue.source() + ": " + issue.message())));
        return report.successful() ? 1 : 0;
    }

    private static ChoicerVoicerMod mod() {
        return ChoicerVoicerMod.instance();
    }
}
