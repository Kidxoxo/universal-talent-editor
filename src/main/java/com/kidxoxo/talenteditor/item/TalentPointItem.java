package com.kidxoxo.talenteditor.item;

import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Built-in consumable which is intentionally independent of world data definitions. */
public final class TalentPointItem extends SystemConfiguredItem {
    public TalentPointItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);
        int before = PlayerTalents.drawTokens(serverPlayer);
        int after = PlayerTalents.addDrawTokens(serverPlayer, 1);
        if (after <= before) {
            serverPlayer.displayClientMessage(
                    Component.translatable("item.talent_editor.talent_point.full"), true
            );
            return InteractionResultHolder.fail(stack);
        }
        if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
        serverPlayer.level().playSound(null, serverPlayer.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.7F, 1.2F);
        serverPlayer.displayClientMessage(
                Component.translatable("item.talent_editor.talent_point.used", after), true
        );
        EditorNetwork.sendPlayerState(serverPlayer, false);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
