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
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

/**
 * The BlockLoc plugin class
 */
public final class BlockLocPlugin extends JavaPlugin {

    private WorldEditPlugin worldEdit;

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
            commands.register(literal("blockloc")
                    .then(argument("material", ArgumentTypes.resource(RegistryKey.BLOCK))
                                    .executes(ctx -> {
                                        final Player player = (Player) ctx.getSource().getSender();
                                        final LocalSession localSession = worldEdit.getSession(player);
                                        final World selectionWorld = localSession.getSelectionWorld();
                                        Material material = ctx.getArgument("material", BlockType.class).asMaterial();
                                        final Region region;

                                        // Attempt to grab region from WE, if it isn't complete let the player know and return
                                        try {
                                            region = localSession.getSelection(selectionWorld);
                                        } catch (final IncompleteRegionException exception) {
                                            player.sendMessage(Component.text("You must select a full region to use this command.").color(NamedTextColor.RED));
                                            return Command.SINGLE_SUCCESS;
                                        }

                                        // Tell the player we're calculating blocks
                                        player.sendMessage(Component.text("Calculating blocks...").color(NamedTextColor.GRAY));

                                        final List<String> locations = getLocations(selectionWorld, region, material);

                                        // If list is empty, tell player no blocks were found and return
                                        if (locations.isEmpty()) {
                                            player.sendMessage(Component.text("There were no blocks of type ").color(NamedTextColor.GRAY)
                                                    .append(Component.text(material.name()).decorate(TextDecoration.BOLD).color(NamedTextColor.YELLOW))
                                                    .append(Component.text(" in your selection.").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                                            );
                                            return Command.SINGLE_SUCCESS;
                                        }

                                        // Notify player of console print
                                        player.sendMessage(Component.text("The block location list has been printed to the console.").color(NamedTextColor.GRAY));

                                        // Print to console
                                        String headerText = "---------- Locations of " + material.name() + " in " + selectionWorld.getName() + " (" + locations.size() + ")" + " ----------";
                                        this.getLogger().info(headerText);
                                        locations.forEach(location -> this.getLogger().info(location));
                                        this.getLogger().info("-".repeat(headerText.length()));
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .build()
                    ).build());
        });
    }

    private List<String> getLocations(World world, Region region, Material material) {
        final Set<String> blockLocationStrings = new HashSet<>();

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

        return new ArrayList<>(blockLocationStrings);
    }
}
