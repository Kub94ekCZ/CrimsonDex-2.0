package me.kub94ek.command.impl.commands;

import me.kub94ek.command.Command;
import me.kub94ek.command.CommandExecutor;
import me.kub94ek.command.impl.executors.CardCommandExecutor;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.Map;

public class CardCommand implements Command {
    
    private static final CardCommandExecutor executor = new CardCommandExecutor();
    
    @Override
    public CommandExecutor getExecutor() {
        return executor;
    }
    
    @Override
    public SlashCommandData getCommand() {
        SlashCommandData command = Commands.slash("card", "Command for card manipulation");
        //command.setDescriptionLocalizations(getDescriptionTranslations());
        command.addSubcommands(
                new SubcommandData("list", "List your cards")
                        .setDescriptionLocalizations(Map.of(
                                DiscordLocale.CZECH, "Ukáže seznam vašich karet",
                                DiscordLocale.SPANISH, "Muestra una lista de sus tarjetas"
                        )),
                new SubcommandData("give", "Give your card to somebody")
                        .addOptions(
                                new OptionData(
                                        OptionType.USER,
                                        "user", "The user to give the card to",
                                        true
                                ).setDescriptionLocalizations(
                                        Map.of(
                                                DiscordLocale.CZECH, "Komu kartu darovat",
                                                DiscordLocale.SPANISH, "A quién regalar la tarjeta"
                                        )
                                ),
                                new OptionData(
                                        OptionType.STRING,
                                        "id", "ID of the card to give (without the #)",
                                        true
                                ).setDescriptionLocalizations(
                                        Map.of(
                                                DiscordLocale.CZECH, "ID karty",
                                                DiscordLocale.SPANISH, "Tarjeta ID"
                                        )
                                )
                        )
                        .setDescriptionLocalizations(Map.of(
                                DiscordLocale.CZECH, "Darujte někomu vaši kartu",
                                DiscordLocale.SPANISH, "Regale su tarjeta a alguien"
                        )),
                new SubcommandData("last", "Shows you the last caught card")
                        .setDescriptionLocalizations(Map.of(
                                DiscordLocale.CZECH, "Ukáže vaši poslední získanou kartu",
                                DiscordLocale.SPANISH, "Muestra su última tarjeta repartida"
                        ))
        );
        
        return command;
    }
    
    /*private Map<DiscordLocale, String> getDescriptionTranslations() {
        return Map.of(
                DiscordLocale.CZECH, "Příkaz pro manipulaci s kartami",
                DiscordLocale.GERMAN, "Command für Kartenmanipulation",
                DiscordLocale.SPANISH, "Comando para manipulación de tarjetas"
        );
    }*/
    
}
