package me.kub94ek.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface CommandExecutor {
    
    void executeCommand(SlashCommandInteractionEvent e);
    
}
