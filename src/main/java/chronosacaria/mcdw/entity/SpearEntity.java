package chronosacaria.mcdw.entity;

import chronosacaria.mcdw.bases.McdwLong;

import chronosacaria.mcdw.network.S2CEntitySpawnPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.ProjectileDamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Packet;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpearEntity extends PersistentProjectileEntity {
    private static final TrackedData<Boolean> ENCHANTMENT_GLINT;
    private ItemStack spearStack;
    private final Set<UUID> piercedEntities = new HashSet<>();

    public SpearEntity(EntityType<? extends SpearEntity> entityType, World world, McdwLong item){
        super(entityType, world);
        this.spearStack = new ItemStack(item);
    }

    public SpearEntity(World world, LivingEntity owner, McdwLong item, ItemStack stack){
        super(item.getType(), owner, world);
        this.spearStack = new ItemStack(item);
        this.spearStack = stack.copy();
        this.dataTracker.set(ENCHANTMENT_GLINT, stack.hasGlint());
    }

    @Environment(EnvType.CLIENT)
    public SpearEntity(World world, double x, double y, double z, McdwLong item) {
        super(item.getType(), x, y, z, world);
        this.spearStack = new ItemStack(item);
    }

    @Override
    protected void initDataTracker(){
        super.initDataTracker();
        this.dataTracker.startTracking(ENCHANTMENT_GLINT, false);
    }

    @Override
    public Packet<?> createSpawnPacket(){
        return S2CEntitySpawnPacket.createPacket(this);
    }

    @Override
    protected ItemStack asItemStack(){
        return this.spearStack.copy();
    }

    @Environment(EnvType.CLIENT)
    public boolean method_23751(){
        return this.dataTracker.get(ENCHANTMENT_GLINT);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult){
        int level = EnchantmentHelper.getLevel(Enchantments.PIERCING, this.spearStack);
        Entity hitEntity = entityHitResult.getEntity();
        if (this.piercedEntities.contains(hitEntity.getUuid()) || this.piercedEntities.size() > level) {
            return;
        }
        this.piercedEntities.add(hitEntity.getUuid());
        float damage = ((McdwLong) this.spearStack.getItem()).getAttackDamage() * 2;
        if (hitEntity instanceof LivingEntity){
            int impalingLevel = EnchantmentHelper.getLevel(Enchantments.IMPALING, this.spearStack);
            if (impalingLevel > 0) {
                damage += impalingLevel * 1.5F;
            }
        }

        Entity owner = this.getOwner();
        DamageSource damageSource = createSpearDamageSource(this, owner == null ? this : owner);
        SoundEvent soundEvent = SoundEvents.ITEM_TRIDENT_HIT;
        if (hitEntity.damage(damageSource, damage)){
            if (hitEntity.getType() == EntityType.ENDERMAN){
                return;
            }

            if (hitEntity instanceof LivingEntity){
                LivingEntity hitLivingEntity = (LivingEntity) hitEntity;
                if (owner instanceof LivingEntity){
                    EnchantmentHelper.onUserDamaged(hitLivingEntity, owner);
                    EnchantmentHelper.onTargetDamaged((LivingEntity) owner, hitLivingEntity);
                }

                this.onHit(hitLivingEntity);
            }
        }

        if (this.piercedEntities.size() > level){
            this.setVelocity(this.getVelocity().multiply(-0.01D, -0.1D, -0.01D));
        } else {
            this.setVelocity(this.getVelocity().multiply(0.75));
        }
        this.playSound(soundEvent, 1.0F, 1.0F);
    }

    @Override
    protected SoundEvent getHitSound(){
        return SoundEvents.ITEM_TRIDENT_HIT_GROUND;
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        Entity entity = this.getOwner();
        if (entity == null || entity.getUuid() == player.getUuid()) {
            super.onPlayerCollision(player);
        }
    }

    @Override
    public void readCustomDataFromTag(CompoundTag tag) {
        super.readCustomDataFromTag(tag);
        if (tag.contains("Item", 10)) {
            this.spearStack = ItemStack.fromTag(tag.getCompound("Item"));
            this.dataTracker.set(ENCHANTMENT_GLINT, this.spearStack.hasGlint());
        }

        this.piercedEntities.clear();
        if (tag.contains("HitEntities", 9)) {
            for (Tag hitEntity : tag.getList("HitEntities", 10)) {
                this.piercedEntities.add(((CompoundTag) hitEntity).getUuid("UUID"));
            }
        }
    }

    @Override
    public void writeCustomDataToTag(CompoundTag tag) {
        super.writeCustomDataToTag(tag);
        tag.put("Item", this.spearStack.toTag(new CompoundTag()));

        ListTag tags = new ListTag();
        for (UUID uuid : this.piercedEntities){
            CompoundTag c = new CompoundTag();
            c.putUuid("UUID", uuid);
            tags.add(c);
        }
        tag.put("HitEntities", tags);
    }

    @Override
    public void age() {
        //The below code was autocorrected, might cause an issue, BE AWARE
        if (this.pickupType != PickupPermission.ALLOWED) {
            super.age();
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    static {
        ENCHANTMENT_GLINT = DataTracker.registerData(net.minecraft.entity.projectile.TridentEntity.class,
                TrackedDataHandlerRegistry.BOOLEAN);
    }

    public static DamageSource createSpearDamageSource(Entity spear, Entity owner){
        return new ProjectileDamageSource("spear", spear, owner).setProjectile();
    }
}
