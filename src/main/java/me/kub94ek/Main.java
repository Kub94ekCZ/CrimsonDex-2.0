package me.kub94ek;

import me.kub94ek.card.Card;
import me.kub94ek.card.CardType;
import me.kub94ek.command.CommandManager;
import me.kub94ek.command.impl.commands.CardCommand;
import me.kub94ek.command.impl.commands.CoinCommand;
import me.kub94ek.command.impl.executors.CardCommandExecutor;
import me.kub94ek.data.database.Database;
import me.kub94ek.data.stats.Stats;
import me.kub94ek.image.CardCreator;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends ListenerAdapter {
    private static JDA jda;
    private static Database database;
    private static CommandManager commandManager;
    private static final List<String> availableChannels = new ArrayList<>();
    
    private static final List<String> messageIds = new ArrayList<>();
    
    private static final HashMap<String, Button> buttons = new HashMap<>();
    private static final HashMap<String, CardType> cardTypes = new HashMap<>();
    private static final HashMap<String, List<String>> rightAnswers = new HashMap<>();
    
    private static final HashMap<String, String> startedBattleIds = new HashMap<>();
    
    private static final List<String> whitelistedChannels = List.of("1274350116379164793");
    private static final ScheduledExecutorService service = Executors.newScheduledThreadPool(0);
    
    public static void main(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("Missing arguments");
        }
        
        database = new Database();
        
        new Main().startBot(args[0]);
        
    }
    
    public void startBot(String botToken) {
        JDABuilder builder = JDABuilder.createLight(
                botToken,
                GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS
        );
        
        builder.setActivity(Activity.customStatus("Throwing CrimsonBalls on the table"));
        
        try {
            jda = builder.build().awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        jda.addEventListener(this);
        
        commandManager = new CommandManager(jda);
        jda.addEventListener(commandManager);
        
        registerGuildCommands();
        
        
        availableChannels.addAll(whitelistedChannels);
    }
    
    private static FileUpload createFileUpload(String fileName) {
        ClassLoader classLoader = Main.class.getClassLoader();

        InputStream is = classLoader.getResourceAsStream(fileName);
        if (is == null) {
            throw new IllegalArgumentException("File not found: " + fileName);
        }
        return FileUpload.fromData(is, "image.png");
    }
    
    
    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (whitelistedChannels.contains(e.getGuildChannel().getId()) && !e.getAuthor().isBot() && !e.getAuthor().isSystem()) {
            if (availableChannels.contains(e.getGuildChannel().getId())) {
                availableChannels.remove(e.getGuildChannel().getId());
                
                spawnCard(e.getGuildChannel().getId());
                
                service.schedule(() -> {
                    availableChannels.add(e.getGuildChannel().getId());
                }, 5, TimeUnit.MINUTES);
            }
        }
    }
    
    private static void spawnCard(String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        Random random = new Random();
        CardType type = CardType.values()[random.nextInt(CardType.values().length)];
        while (!type.obtainable) {
            type = CardType.values()[random.nextInt(CardType.values().length)];
        }
        Button button = Button.primary("catch", "Catch CrimsonBall!");
        
        final CardType finalType = type;
        channel.sendMessage(
                        "New CrimsonBall has spawned \n\n"
                )
                .addFiles(
                        createFileUpload("image/spawn/" + type.imageName + ".png")
                )
                .addActionRow(
                        button
                ).queue(message -> {
                    new File("image.png").delete();
                    var messageId = message.getId();
                    
                    buttons.put(messageId, button);
                    messageIds.add(messageId);
                    rightAnswers.put(channelId, finalType.validAnswers);
                    cardTypes.put(messageId, finalType);
                    
                    service.schedule(() -> {
                        if (buttons.containsKey(messageId)) {
                            message.editMessageComponents(ActionRow.of(button.asDisabled())).queue();
                            buttons.remove(messageId);
                        }
                        messageIds.remove(messageId);
                    }, 3, TimeUnit.MINUTES);
                });
    }
    
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (event.getComponentId().startsWith("card-list")) {
            final CardType[] cardType = {null};
            final int[] stats = {0, 0};
            
            StringBuilder messageBuilder = new StringBuilder();
            database.getUserCards(event.getMember().getId()).forEach((card) -> {
                if (card.getId().equals(event.getInteraction().getSelectedOptions().getFirst().getValue())) {
                    messageBuilder.append(card);
                    cardType[0] = card.getType();
                    stats[0] = card.getAtkBonus();
                    stats[1] = card.getHpBonus();
                }
            });
            
            try {
                CardCreator.createCardImage(new Card("id", "owner", cardType[0], stats[0], stats[1]));
            } catch (IOException | FontFormatException e) {
                e.printStackTrace();
            }
            
            event.reply(messageBuilder.toString())
                    .addFiles(FileUpload.fromData(
                        new File("image.jpg")
                        ))
                    .setEphemeral(true)
                    .queue();
            
        }
    }
    
    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        //String memberId = dms.get(e.getChannelId());
        
        if (e.getComponentId().equals("catch")) {
            if (!messageIds.contains(e.getMessage().getId())) {
                e.reply("This button can't be used").setEphemeral(true).queue();
                return;
            }
            
            TextInput answer = TextInput.create("answer", "Answer", TextInputStyle.SHORT)
                    .setPlaceholder("Type the answer here")
                    .setMinLength(1)
                    .setMaxLength(100)
                    .build();
            
            Modal modal = Modal.create("catch", "Catch the card")
                    .addComponents(ActionRow.of(answer))
                    .build();
            
            e.replyModal(modal).queue();
        } else if (e.getComponentId().equals("previous-page")) {
            if (CardCommandExecutor.pages.containsKey(e.getMember().getId())) {
                int page = CardCommandExecutor.pages.get(e.getMember().getId()) - 1;
                e.getMessage().delete().queue();
                var message = CardCommandExecutor.createListMessage(
                        e.reply("Listing all your cards:"),
                        database.getUserCards(e.getMember().getId()),
                        page);
                message.setEphemeral(true).queue(sentMessage -> CardCommandExecutor.pages.put(sentMessage.getId(), page));
            }
        } else if (e.getComponentId().equals("next-page")) {
            if (CardCommandExecutor.pages.containsKey(e.getMember().getId())) {
                int page = CardCommandExecutor.pages.get(e.getMember().getId()) + 1;
                e.getMessage().delete().queue();
                var message = CardCommandExecutor.createListMessage(
                        e.reply("Listing all your cards:"),
                        database.getUserCards(e.getMember().getId()),
                        page);
                message.setEphemeral(true).queue(sentMessage -> CardCommandExecutor.pages.put(e.getMember().getId(), page));
            }
        }
    }
    
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String memberId = event.getMember().getId();
        
        if (event.getModalId().equals("catch")) {
            String answer = event.getValue("answer").getAsString();
            if (!messageIds.contains(event.getMessage().getId())) {
                event.reply("This card was already caught.").setEphemeral(true).queue();
                return;
            }
            
            if (rightAnswers.get(event.getChannelId()).contains(answer.toLowerCase())) {
                if (!messageIds.contains(event.getMessage().getId())) {
                    event.reply("This card was already caught.").setEphemeral(true).queue();
                    return;
                }
                
                messageIds.remove(event.getMessage().getId());
                Random random = new Random();
                CardType type = cardTypes.get(event.getMessage().getId());
                
                Card card = new Card(Generator.createUniqueCardId(), memberId, type,
                        random.nextInt(-20, 21),
                        random.nextInt(-20, 21));
                
                /*CardCatchEvent cardCatchEvent = new CardCatchEvent(card, event.getMember().getId());
                
                if (EventManager.dispatchEvent(cardCatchEvent)) {
                    event.reply(cardCatchEvent.getMessage()).setEphemeral(true).queue();
                    return;
                }*/
                
                event.reply("Correct!").setEphemeral(true).queue();
                event.getMessageChannel().sendMessage(event.getMember().getAsMention() +
                        " Has caught a CrimsonBall.\n" + card).queue();
                cardTypes.remove(event.getMessage().getId());
                event.getMessage().editMessageComponents(
                        ActionRow.of(buttons.get(event.getMessage().getId()).asDisabled())
                ).queue();
                buttons.remove(event.getMessage().getId());
                
                try {
                    database.addCard(card);
                    if (!database.hasStats(memberId)) {
                        database.initStats(memberId);
                        database.setStat(
                                memberId,
                                Stats.CARDS_CAUGHT,
                                database.getUserCards(memberId).size()
                        );
                    } else {
                        database.increaseStat(memberId, Stats.CARDS_CAUGHT, 1);
                    }
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                
            } else {
                event.reply("Wrong Answer!").setEphemeral(true).queue();
            }
        }
    }
    
    private void registerGuildCommands() {
        jda.getGuilds().forEach(guild -> guild.retrieveCommands().queue(commands -> commands.forEach(command -> {
            if (command.getApplicationId().equals("1274368616179175485")) {
                command.delete().queue();
            }
        })));
        
        commandManager.registerCommands(List.of(new CardCommand(), new CoinCommand()));
    }
    
    public static JDA getJda() {
        return jda;
    }
    public static Database getDatabase() {
        return database;
    }
    
    public static HashMap<String, String> getStartedBattleIds() {
        return startedBattleIds;
    }
    
}