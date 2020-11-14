package dev.kscott.blockloc;

import cloud.commandframework.Command;
import cloud.commandframework.CommandTree;
import cloud.commandframework.Description;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class BlockLoc extends JavaPlugin {

    private PaperCommandManager<CommandSender> manager;
    private WorldEditPlugin worldEdit;
    private BukkitAudiences bukkitAudiences;
    private List<String> materialNames;

    @Override
    public void onEnable() {

        materialNames = new ArrayList<>();

        for (var mat : Material.values()) {
            materialNames.add(mat.name());
        }

        this.bukkitAudiences = BukkitAudiences.create(this);

        // Cloud stuff
        final Function<CommandTree<CommandSender>, CommandExecutionCoordinator<CommandSender>> executionCoordinatorFunction =
                AsynchronousCommandExecutionCoordinator.<CommandSender>newBuilder().build();
        final Function<CommandSender, CommandSender> mapperFunction = Function.identity();

        try {
            this.manager = new PaperCommandManager<>(
                    this,
                    executionCoordinatorFunction,
                    mapperFunction,
                    mapperFunction
            );
        } catch (final Exception e) {
            this.getLogger().severe("Failed to initialize cloud");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");

        constructCommands();

    }

    private void constructCommands() {
        final Command.Builder<CommandSender> builder = this.manager.commandBuilder("blockloc");
        this.manager.command(builder.literal("getType")
                .senderType(Player.class)
                .argument(StringArgument.<CommandSender>newBuilder("material")
                        .withSuggestionsProvider((ctx, str) -> materialNames)
                        .build()
                )
                .handler(ctx -> manager.taskRecipe().begin(ctx)
                        .asynchronous(cmdCtx -> {
                            final Player player = (Player) cmdCtx.getSender();
                            final Audience playerAudience = this.bukkitAudiences.player(player);
                            final Material material;
                            try {
                                material = Material.valueOf(cmdCtx.get("material"));
                            } catch (final IllegalArgumentException exception) {
                                playerAudience.sendMessage(Component.text("The material ").color(NamedTextColor.RED)
                                        .append(Component.text((String) cmdCtx.get("material")).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                                        .append(Component.text(" could not be found. (Did you use CAPS?)").decoration(TextDecoration.BOLD, false).color(NamedTextColor.RED)));
                                return;
                            }

                            final LocalSession localSession = worldEdit.getSession(player);
                            final World selectionWorld = localSession.getSelectionWorld();
                            final Region region;
                            try {
                                region = localSession.getSelection(selectionWorld);
                            } catch (final IncompleteRegionException exception) {
                                playerAudience.sendMessage(Component.text("You must select a full region to use this command.").color(NamedTextColor.RED));
                                return;
                            }

                            playerAudience.sendMessage(Component.text("Calculating blocks...").color(NamedTextColor.GRAY));

                            final @NonNull Set<String> blockLocationStrings = new HashSet<>();

                            final @NonNull BlockVector3 vecMax = region.getMaximumPoint();
                            final @NonNull BlockVector3 vecMin = region.getMinimumPoint();

                            final int xMax = vecMax.getBlockX();
                            final int yMax = vecMax.getBlockY();
                            final int zMax = vecMax.getBlockZ();

                            final int xMin = vecMin.getBlockX();
                            final int yMin = vecMin.getBlockY();
                            final int zMin = vecMin.getBlockZ();

                            for (int i = xMin; i < xMax; i++) {
                                for (int j = yMin; j < yMax; j++) {
                                    for (int k = zMin; k < zMax; k++) {
                                        BlockVector3 blockVector3 = BlockVector3.at(i, j, k);
                                        BlockState blockState = selectionWorld.getBlock(blockVector3);

                                        String id = blockState.getBlockType().getId();

                                        if (id.equals(material.getKey().toString())) {
                                            blockLocationStrings.add(blockVector3.getBlockX()+", "+blockVector3.getBlockY()+", "+blockVector3.getBlockZ());
                                        }
                                    }
                                }
                            }

                            if (blockLocationStrings.isEmpty()) {
                                playerAudience.sendMessage(Component.text("There were no blocks of type ").color(NamedTextColor.GRAY)
                                        .append(Component.text(material.name()).decorate(TextDecoration.BOLD).color(NamedTextColor.YELLOW))
                                        .append(Component.text(" in your selection.").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                                );
                            } else {
                                playerAudience.sendMessage(Component.text("The block location list has been printed to the console.").color(NamedTextColor.GRAY));
                                String headerText = "---------- Locations of "+material.name()+" in "+selectionWorld.getName()+" ----------";
                                this.getLogger().info(headerText);
                                blockLocationStrings.forEach(location -> {
                                    this.getLogger().info(location);
                                });
                                this.getLogger().info("-".repeat(headerText.length()));
                            }

                        }).execute()
                )
        );
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
