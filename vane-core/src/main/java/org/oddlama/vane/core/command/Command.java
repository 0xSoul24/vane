package org.oddlama.vane.core.command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static org.oddlama.vane.util.ArrayUtil.prepend;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.VaneCommand;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.params.AnyParam;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

@VaneCommand
public abstract class Command<T extends Module<T>> extends ModuleComponent<T> {

    public class BukkitCommand extends org.bukkit.command.Command implements PluginIdentifiableCommand {

        public BukkitCommand(String name) {
            super(name);
            setPermission(Command.this.permission.getName());
        }

        @Override
        public String getUsage() {
            return Command.this.langUsage.str("§7/§3" + name);
        }

        @Override
        public String getDescription() {
            return Command.this.langDescription.str();
        }

        @Override
        public Plugin getPlugin() {
            return Command.this.getModule();
        }

        @Override
        public boolean execute(CommandSender sender, String alias, String[] args) {
            System.out.println("exec " + alias + " from " + sender);
            // Pre-check permission
            if (!sender.hasPermission(Command.this.permission)) {
                getModule().core.langCommandPermissionDenied.send(sender);
                System.out.println("no perms!");
                return true;
            }

            // Ambiguous matches will always execute the
            // first chain based on definition order.
            try {
                return rootParam.checkAccept(sender, prepend(args, alias), 0).apply(Command.this, sender);
            } catch (Exception e) {
                sender.sendMessage(
                    "§cAn unexpected error occurred. Please examine the console log and/or notify a server administrator."
                );
                throw e;
            }
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args)
            throws IllegalArgumentException {
            // Don't allow information exfiltration!
            if (!sender.hasPermission(getPermission())) {
                return Collections.emptyList();
            }

            try {
                return rootParam.buildCompletions(sender, prepend(args, alias), 0);
            } catch (Exception e) {
                sender.sendMessage(
                    "§cAn unexpected error occurred. Please examine the console log and/or notify a server administrator."
                );
                throw e;
            }
        }
    }

    // Language
    @LangMessage
    public TranslatedMessage langUsage;

    @LangMessage
    public TranslatedMessage langDescription;

    @LangMessage
    public TranslatedMessage langHelp;

    // Variables
    private String name;
    private Permission permission;
    private BukkitCommand bukkitCommand;

    // Root parameter
    private AnyParam<String> rootParam;

    private LiteralArgumentBuilder<CommandSourceStack> brigadierCommand;
    private Aliases aliases;

    public Command(Context<T> context) {
        this(context, PermissionDefault.OP);
    }

    public Command(Context<T> context, PermissionDefault permissionDefault) {
        super(null);
        // Make namespace
        name = getClass().getAnnotation(Name.class).value();
        // Convert name to PascalCase for the group name
        String groupName = "Command" + name.substring(0, 1).toUpperCase() + name.substring(1);
        context = context.group(groupName, "Enable command " + name);
        setContext(context);

        // Register permission
        permission = new Permission(
            "vane." + getModule().getAnnotationName() + ".commands." + name,
            "Allow access to /" + name,
            permissionDefault
        );
        getModule().registerPermission(permission);
        permission.addParent(getModule().permissionCommandCatchallModule, true);
        permission.addParent(getModule().core.permissionCommandCatchall, true);

        // Always allow the console to execute commands
        getModule().addConsolePermission(permission);

        // Initialize root parameter
        rootParam = new AnyParam<String>(this, "/" + getName(), str -> str);

        // Create bukkit command
        bukkitCommand = new BukkitCommand(name);
        bukkitCommand.setLabel(name);
        bukkitCommand.setName(name);

        aliases = getClass().getAnnotation(Aliases.class);
        brigadierCommand = Commands.literal(name);
        if (aliases != null) {
            bukkitCommand.setAliases(List.of(aliases.value()));
        }
    }

    public BukkitCommand getBukkitCommand() {
        return bukkitCommand;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission.getName();
    }

    public String getPrefix() {
        return "vane:" + getModule().getAnnotationName();
    }

    public Param params() {
        return rootParam;
    }

    public LiteralArgumentBuilder<CommandSourceStack> getCommandBase() {
        return brigadierCommand;
    }

    public LiteralCommandNode<CommandSourceStack> getCommand() {
        var cmd = getCommandBase();
        var oldRequirement = cmd.getRequirement();
        return cmd
            .requires(stack -> stack.getSender().hasPermission(permission) && oldRequirement.test(stack))
            .build();
    }

    public List<String> getAliases() {
        if (aliases != null && aliases.value().length > 0) {
            return List.of(aliases.value());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected void onEnable() {
        getModule().registerCommand(this);
    }

    @Override
    protected void onDisable() {
        getModule().unregisterCommand(this);
    }

    public void printHelp(CommandSender sender) {
        langUsage.send(sender, "§7/§3" + name);
        langHelp.send(sender);
    }

    public int printHelp2(CommandContext<CommandSourceStack> ctx) {
        langUsage.send(ctx.getSource().getSender(), "§7/§3" + name);
        langHelp.send(ctx.getSource().getSender());
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    public LiteralArgumentBuilder<CommandSourceStack> help() {
        return literal("help").executes(ctx -> {
            printHelp2(ctx);
            return SINGLE_SUCCESS;
        });
    }
}
