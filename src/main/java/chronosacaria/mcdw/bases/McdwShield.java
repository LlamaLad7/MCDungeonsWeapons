package chronosacaria.mcdw.bases;

import chronosacaria.mcdw.Mcdw;
import chronosacaria.mcdw.api.util.CleanlinessHelper;
import chronosacaria.mcdw.api.util.RarityHelper;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class McdwShield extends ShieldItem {

    public final ToolMaterial material;
    String[] repairIngredient;

    public McdwShield(ToolMaterial material, String[] repairIngredient) {
        super(new Item.Settings().rarity(RarityHelper.fromToolMaterial(material)).maxCount(1)
                .maxDamage(250 + material.getDurability())
        );
        ItemGroupEvents.modifyEntriesEvent(Mcdw.SHIELDS).register(entries -> entries.add(this));
        this.material = material;
        this.repairIngredient = repairIngredient;

        DispenserBlock.registerBehavior(this, ArmorItem.DISPENSER_BEHAVIOR);
    }

    @Override
    public String getTranslationKey (ItemStack itemStack){
        return BlockItem.getBlockEntityNbt(itemStack) != null ?
                this.getTranslationKey() + '.' + getColor(itemStack).getName() : super.getTranslationKey(itemStack);
    }

    @Override
    public UseAction getUseAction(ItemStack itemStack){
        return UseAction.BLOCK;
    }

    @Override
    public int getMaxUseTime(ItemStack itemStack){
        return 72000;
    }

    @Override
    public TypedActionResult<ItemStack> use (World world, PlayerEntity user, Hand hand){
        ItemStack itemStack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return CleanlinessHelper.canRepairCheck(repairIngredient, ingredient);
    }
}