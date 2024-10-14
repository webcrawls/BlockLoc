package sh.kaden.blockloc;

import com.mojang.brigadier.Command;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The BlockLoc plugin class
 */
public final class BlockLocPlugin extends JavaPlugin {

//    /**
//     * cloud PaperCommandManager instance
//     */
//    private @MonotonicNonNull PaperCommandManager<CommandSender> manager;
//
    /**
     * WorldEdit plugin instance
     */
    private @MonotonicNonNull WorldEditPlugin worldEdit;
//
//    /**
//     * BukkitAudiences instance
//     */
//    private @MonotonicNonNull BukkitAudiences bukkitAudiences;

    /**
     * Create audience, manager, WorldEdit plugin, and register commands.
     */
    @Override
    public void onEnable() {
        this.worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");

        if (this.worldEdit == null) {
            this.getLogger().severe("WorldEdit was not found!");
            this.getServer().getPluginManager().disablePlugin(this);
        }

        this.registerBrigadierCommands();
    }

    private void registerBrigadierCommands() {
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("search")
                            .executes(ctx -> {
                                ctx.getSource().getSender().sendPlainMessage("not searching!! lololol");
                                return Command.SINGLE_SUCCESS;
                            })
                            .build(),
                    "some bukkit help description string",
                    List.of("an-alias")
            );
        });
    }

//    /**
//     * Construct exception handlers
//     */
//    private void constructExceptionHandlers() {
//        manager.registerExceptionHandler(ArgumentParseException.class, (ctx, ex) -> {
//            // If the error is a MaterialParseException, tell the player we could not find the material.
//            if (ex.getCause() instanceof MaterialArgument.MaterialParseException) {
//                final Player player = (Player) ex.getCommandSender();
//                final Audience audience = bukkitAudiences.player(player);
//
//                audience.sendMessage(Component.text("The material ").color(NamedTextColor.RED)
//                        .append(Component.text(((MaterialArgument.MaterialParseException) ex.getCause()).getInput()).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
//                        .append(Component.text(" could not be found. (Did you use CAPS?) hi jmp").decoration(TextDecoration.BOLD, false).color(NamedTextColor.RED)));
//            }
//        });
//    }

//    /**
//     * Construct commands
//     */
//    private void constructCommands() {
//        final Command.Builder<CommandSender> builder = this.manager.commandBuilder("blockloc");
//        this.manager.command(builder.literal("getType")
//                .senderType(Player.class)
//                .argument(MaterialArgument.of("material"))
//                .handler(ctx -> manager.taskRecipe().begin(ctx).asynchronous(this::handleGetType).execute())
//        );
//    }

//    /**
//     * Handle /blockloc getType
//     *
//     * @param ctx command context
//     */
//    private void handleGetType(final @NonNull CommandContext<CommandSender> ctx) {
//        final @NonNull Player player = (Player) ctx.getSender();
//        final @NonNull Audience playerAudience = this.bukkitAudiences.player(player);
//        final @NonNull Material material = ctx.get("material");
//
//        final @NonNull LocalSession localSession = worldEdit.getSession(player);
//        final @NonNull World selectionWorld = localSession.getSelectionWorld();
//        final @NonNull Region region;
//
//        // Attempt to grab region from WE, if it isn't complete let the player know and return
//        try {
//            region = localSession.getSelection(selectionWorld);
//        } catch (final IncompleteRegionException exception) {
//            playerAudience.sendMessage(Component.text("You must select a full region to use this command.").color(NamedTextColor.RED));
//            return;
//        }
//
//        // Tell the player we're calculating blocks
//        playerAudience.sendMessage(Component.text("Calculating blocks...").color(NamedTextColor.GRAY));
//
//
//        // If list is empty, tell player no blocks were found and return
//        if (blockLocationStrings.isEmpty()) {
//            playerAudience.sendMessage(Component.text("There were no blocks of type ").color(NamedTextColor.GRAY)
//                    .append(Component.text(material.name()).decorate(TextDecoration.BOLD).color(NamedTextColor.YELLOW))
//                    .append(Component.text(" in your selection.").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
//            );
//            return;
//        }
//
//        // Notify player of console print
//        playerAudience.sendMessage(Component.text("The block location list has been printed to the console.").color(NamedTextColor.GRAY));
//
//        // Print to console
//        String headerText = "---------- Locations of " + material.name() + " in " + selectionWorld.getName() + " (" + blockLocationStrings.size() + ")" + " ----------";
//        this.getLogger().info(headerText);
//        blockLocationStrings.forEach(location -> this.getLogger().info(location));
//        this.getLogger().info("-".repeat(headerText.length()));
//    }

    private List<Location> getLocations(World world, Region region, Material material) {
        final @NonNull Set<String> blockLocationStrings = new HashSet<>();

        final @NonNull BlockVector3 vecMax = region.getMaximumPoint();
        final @NonNull BlockVector3 vecMin = region.getMinimumPoint();

        final int xMax = vecMax.x();
        final int yMax = vecMax.y();
        final int zMax = vecMax.z();

        final int xMin = vecMin.x();
        final int yMin = vecMin.y();
        final int zMin = vecMin.z();

        // Loop through xMin-Max, yMin-Max, zMin-Max
        for (int i = xMin; i <= xMax; i++) {
            for (int j = yMin; j <= yMax; j++) {
                for (int k = zMin; k <= zMax; k++) {
                    // Grab block info
                    BlockVector3 blockVector3 = BlockVector3.at(i, j, k);
                    BlockState blockState = world.getBlock(blockVector3);

                    String id = blockState.getBlockType().getId();

                    // If the blockType id matches our material key, add it to the list
                    if (id.equals(material.getKey().toString())) {
                        blockLocationStrings.add(blockVector3.x() + ", " + blockVector3.y() + ", " + blockVector3.z());
                    }
                }
            }
        }

        return List.of();
    }
}
