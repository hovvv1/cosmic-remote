package dev.cosmic.remote;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Clic droit : lance la compétence choisie sur ton viseur.
 * Clic droit en étant accroupi : change de compétence.
 */
public class RemoteItem extends Item {
    public RemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            int next = Skills.cycle(sp);
            sp.sendOverlayMessage(Skills.label(next));
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        HitResult hit = player.pick(Config.data.maxRange, 1.0F, false);
        Vec3 target = hit.getLocation();
        int skill = Skills.selected(sp);

        if (!Skills.cast(serverLevel, sp, skill, target)) {
            sp.sendOverlayMessage(Component.literal("Une compétence est déjà en cours."));
            return InteractionResult.FAIL;
        }

        if (!Config.data.noCooldown && Config.data.cooldownSeconds > 0) {
            player.getCooldowns().addCooldown(stack, Config.data.cooldownSeconds * 20);
        }
        return InteractionResult.SUCCESS;
    }
}
