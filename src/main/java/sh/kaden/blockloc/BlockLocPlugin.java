package sh.kaden.blockloc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

/**
 * Paper plugin entrypoint class
 */
public final class BlockLocPlugin extends JavaPlugin {

    private WorldEditPlugin worldEdit;

    @Override
    public void onEnable() {
        this.worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");

        if (this.worldEdit == null) {
            this.getLogger().severe("WorldEdit was not found!");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.registerCommands();
    }

    private void registerCommands() {
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            final LiteralArgumentBuilder<CommandSourceStack> root = literal("blockloc");
            commands.register(root.executes(this::handleRoot).build());
            commands.register(root.then(argument("material", ArgumentTypes.resource(RegistryKey.BLOCK)).executes(this::handleSearch)).build());
        });
    }

    private int handleSearch(CommandContext<CommandSourceStack> ctx) {
        final Player player = (Player) ctx.getSource().getSender();
        final LocalSession localSession = worldEdit.getSession(player);
        final World selectionWorld = localSession.getSelectionWorld();
        Material material = ctx.getArgument("material", BlockType.class).asMaterial();
        final Region region;

        // Attempt to grab region from WE, if it isn't complete let the player know and return
        try {
            region = localSession.getSelection(selectionWorld);
        } catch (final IncompleteRegionException exception) {
            player.sendMessage(text("You must select a full region to use this command.").color(RED));
            return Command.SINGLE_SUCCESS;
        }

        // Tell the player we're calculating blocks
        player.sendMessage(text("Calculating blocks...").color(GRAY));

        final List<BlockVector3> locations = getLocations(selectionWorld, region, material);

        // If list is empty, tell player no blocks were found and return
        if (locations.isEmpty()) {
            player.sendMessage(text("There were no blocks of type ").color(GRAY)
                    .append(text(material.name()).decorate(BOLD).color(YELLOW))
                    .append(text(" in your selection.").color(GRAY).decoration(BOLD, false))
            );
            return Command.SINGLE_SUCCESS;
        }

        // Notify player of console print
        player.sendMessage(text("The block location list has been printed to the console.").color(GRAY));

        // Print to console
        String headerText = "---------- Locations of " + material.name() + " in " + selectionWorld.getName() + " (" + locations.size() + ")" + " ----------";
        this.getLogger().info(headerText);
        locations.forEach(location -> this.getLogger().info(location.toString()));
        this.getLogger().info("-".repeat(headerText.length()));
        return Command.SINGLE_SUCCESS;
                                        }

    private int handleRoot(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendRichMessage("<red>Hi");
        return Command.SINGLE_SUCCESS;
    }

    private List<BlockVector3> getLocations(World world, Region region, Material material) {
        final Set<BlockVector3> blockLocationStrings = new HashSet<>();

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
                        blockLocationStrings.add(blockVector3);
                    }
                }
            }
        }

        return new ArrayList<>(blockLocationStrings);
    }
}
