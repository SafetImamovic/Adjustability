package io.github.safetimamovic.adjustability;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.TooltipLines;
import io.github.safetimamovic.adjustability.tags.ModTags;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * {@code /adjustability inspect} and {@code /adjustability add <enchantment> [level]} (game masters), on the item in the
 * player's main hand. Registered on each loader.
 */
public final class AdjustabilityCommand
{
        private AdjustabilityCommand()
        {
        }

        public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context)
        {
                dispatcher.register(Commands.literal(Constants.MOD_ID)
                        .then(Commands.literal("inspect")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(AdjustabilityCommand::inspect))
                        .then(Commands.literal("add")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("enchantment",
                                                ResourceArgument.resource(context, Registries.ENCHANTMENT))
                                        .executes(command -> add(command, 1))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                                                .executes(command -> add(command,
                                                        IntegerArgumentType.getInteger(command, "level")))))));
        }

        private static int inspect(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
        {
                CommandSourceStack source = context.getSource();
                ItemStack stack = held(source);
                if (stack.isEmpty())
                {
                        source.sendFailure(Component.translatable("adjustability.command.no_item"));
                        return 0;
                }
                Adjuster.Split split = Adjuster.resplit(stack, ItemEnchantments.EMPTY, source.registryAccess());
                ItemEnchantments active = Adjuster.activeOf(stack);
                ItemEnchantments dormant = Adjuster.dormantOf(stack, source.registryAccess());
                source.sendSuccess(() -> Component.translatable("adjustability.command.inspect.header",
                        stack.getHoverName()).withStyle(ChatFormatting.GOLD), false);
                source.sendSuccess(() -> line("adjustability.command.inspect.active", list(active)), false);
                source.sendSuccess(() -> line("adjustability.command.inspect.dormant", list(dormant)), false);
                List<Set<Holder<Enchantment>>> components = split.graph().components();
                for (Set<Holder<Enchantment>> component : components)
                {
                        String names = component.stream().map(Adjuster::id).collect(Collectors.joining(", "));
                        source.sendSuccess(() -> line("adjustability.command.inspect.conflict",
                                Component.literal(names)), false);
                }
                if (!split.active().equals(active) || !split.dormant().equals(dormant))
                {
                        source.sendSuccess(() -> Component.translatable("adjustability.command.inspect.invalid")
                                .withStyle(ChatFormatting.RED), false);
                }
                return components.size();
        }

        private static int add(CommandContext<CommandSourceStack> context, int level) throws CommandSyntaxException
        {
                CommandSourceStack source = context.getSource();
                Holder<Enchantment> enchantment = ResourceArgument.getEnchantment(context, "enchantment");
                ItemStack stack = held(source);
                if (stack.isEmpty())
                {
                        source.sendFailure(Component.translatable("adjustability.command.no_item"));
                        return 0;
                }
                if (stack.is(ModTags.NOT_ADJUSTABLE))
                {
                        source.sendFailure(Component.translatable("adjustability.command.not_adjustable"));
                        return 0;
                }
                ItemEnchantments.Mutable extra = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                extra.set(enchantment, level);
                Adjuster.Split split = Adjuster.resplit(stack, extra.toImmutable(), source.registryAccess());
                Adjuster.write(stack, split);
                boolean dormant = split.dormant().getLevel(enchantment) > 0;
                source.sendSuccess(() -> Component.translatable(dormant ? "adjustability.command.add.dormant"
                        : "adjustability.command.add.active", TooltipLines.name(enchantment, level)), true);
                return 1;
        }

        private static ItemStack held(CommandSourceStack source) throws CommandSyntaxException
        {
                ServerPlayer player = source.getPlayerOrException();
                return player.getMainHandItem();
        }

        private static MutableComponent line(String key, Component value)
        {
                return Component.translatable(key, value.copy().withStyle(ChatFormatting.WHITE))
                        .withStyle(ChatFormatting.GRAY);
        }

        private static Component list(ItemEnchantments enchantments)
        {
                if (enchantments.isEmpty())
                {
                        return Component.translatable("adjustability.command.inspect.none");
                }
                MutableComponent result = Component.empty();
                boolean first = true;
                for (Holder<Enchantment> enchantment : enchantments.keySet().stream()
                        .sorted((a, b) -> Adjuster.id(a).compareTo(Adjuster.id(b))).toList())
                {
                        if (!first)
                        {
                                result.append(", ");
                        }
                        result.append(TooltipLines.name(enchantment, enchantments.getLevel(enchantment)));
                        first = false;
                }
                return result;
        }
}
