package ballistix.common.entity;

import ballistix.common.blast.Blast;
import ballistix.common.block.subtype.SubtypeBlast;
import ballistix.registers.BallistixEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class EntityMissile extends Entity {

	private static final DataParameter<Integer> TYPE = EntityDataManager.defineId(EntityMissile.class, DataSerializers.INT);
	private static final DataParameter<Integer> RANGE = EntityDataManager.defineId(EntityMissile.class, DataSerializers.INT);
	private static final DataParameter<Integer> ISSTUCK = EntityDataManager.defineId(EntityMissile.class, DataSerializers.INT);
	public BlockPos target;
	public int blastOrdinal = -1;
	public int range = -1;
	public boolean isItem = false;
	private EntityBlast blastEntity = null;
	public boolean isStuck = false;

	public EntityMissile(EntityType<? extends EntityMissile> type, World worldIn) {
		super(type, worldIn);
		blocksBuilding = true;
	}

	public EntityMissile(World worldIn) {
		this(BallistixEntities.ENTITY_MISSILE.get(), worldIn);
	}

	public void setBlastType(SubtypeBlast explosive) {
		blastOrdinal = explosive.ordinal();
	}

	public SubtypeBlast getBlastType() {
		return blastOrdinal == -1 ? null : SubtypeBlast.values()[blastOrdinal];
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(TYPE, -1);
		entityData.define(RANGE, -1);
		entityData.define(ISSTUCK, -1);
	}

	@Override
	public AxisAlignedBB getBoundingBoxForCulling() {
		return super.getBoundingBoxForCulling().expandTowards(20, 20, 20);
	}

	@Override
	public void tick() {
		if (isStuck) {
			if (!level.isClientSide && blastEntity.getBlast().hasStarted) {
				removeAfterChangingDimensions();
			}
			return;
		}
		BlockState state = level.getBlockState(blockPosition());
		if (!level.isClientSide || state.getCollisionShape(level, blockPosition()).isEmpty()) {
			setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
		}
		if (blastEntity == null) {
			if (getDeltaMovement().length() > 0 && !isStuck) {

				float xRot = (float) (Math.atan(getDeltaMovement().y() / Math.sqrt(getDeltaMovement().x() * getDeltaMovement().x() + getDeltaMovement().z() * getDeltaMovement().z())) * 180.0D / Math.PI);
				float yRot = (float) (Math.atan2(getDeltaMovement().x(), getDeltaMovement().z()) * 180.0D / Math.PI);

				setRot(yRot, xRot);
			}
			if (!level.isClientSide) {
				if (!state.getCollisionShape(level, blockPosition()).isEmpty() || !isItem && target != null && getY() < target.getY() && getDeltaMovement().y() < 0) {
					if (blastOrdinal != -1 && (target == null || tickCount > 20)) {
						SubtypeBlast explosive = SubtypeBlast.values()[blastOrdinal];
						setPos(getX() - getDeltaMovement().x * 2, getY() - getDeltaMovement().y * 2, getZ() - getDeltaMovement().z * 2);
						Blast b = Blast.createFromSubtype(explosive, level, blockPosition());
						if (b != null) {
							blastEntity = b.performExplosion();
							if (blastEntity == null) {
								removeAfterChangingDimensions();
							} else {
								isStuck = true;
							}
						}
					}
				}
				if (!isItem && getY() > 500) {
					if (target == null) {
						removeAfterChangingDimensions();
					} else {
						setPos(target.getX(), 499, target.getZ());
						setDeltaMovement(0, -3f, 0);
					}
				}
			}
			if (!level.isClientSide) {
				entityData.set(TYPE, blastOrdinal);
				entityData.set(RANGE, range);
				entityData.set(ISSTUCK, isStuck ? 1 : -1);
			} else {
				blastOrdinal = entityData.get(TYPE);
				range = entityData.get(RANGE);
				boolean old = isStuck;
				isStuck = entityData.get(ISSTUCK) > 0;
				if (isStuck != old) {
					setPos(getX() - getDeltaMovement().x * 1, getY() - getDeltaMovement().y * 1, getZ() - getDeltaMovement().z * 1);
				}
			}
			if (!isItem && target != null && getDeltaMovement().y < 3 && getDeltaMovement().y >= 0) {
				setDeltaMovement(getDeltaMovement().add(0, 0.02, 0));
			}
			for (int i = 0; i < 5; i++) {
				float str = level.random.nextFloat() * 0.25f;
				float ranX = str * (level.random.nextFloat() - 0.5f);
				float ranY = str * (level.random.nextFloat() - 0.5f);
				float ranZ = str * (level.random.nextFloat() - 0.5f);
				float x = (float) (getX() - getDimensionsForge(getPose()).width / 1.0f);
				float y = (float) (getY() + getDimensionsForge(getPose()).height / 1.0f);
				float z = (float) (getZ() - getDimensionsForge(getPose()).width / 1.0f);
				level.addParticle(ParticleTypes.LARGE_SMOKE, x + ranX, y + ranY, z + ranZ, -getDeltaMovement().x + ranX, -getDeltaMovement().y - 0.075f + ranY, -getDeltaMovement().z + ranZ);

			}
			float motionX = (float) -getDeltaMovement().x;
			float motionY = (float) -getDeltaMovement().y;
			float motionZ = (float) -getDeltaMovement().z;
			if (level.isClientSide) {
				for (int i = 0; i < 4; i++) {
					level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, false, this.getX(), this.getY(), this.getZ(), random.nextDouble() / 1.5 - 0.3333 + motionX, random.nextDouble() / 1.5 - 0.3333 + motionY, random.nextDouble() / 1.5 - 0.3333 + motionZ);
				}
				for (int i = 0; i < 4; i++) {
					level.addParticle(ParticleTypes.CLOUD, false, this.getX(), this.getY(), this.getZ(), random.nextDouble() / 1.5 - 0.3333 + motionX, random.nextDouble() / 1.5 - 0.3333 + motionY, random.nextDouble() / 1.5 - 0.3333 + motionZ);
				}
			}

		}
	}

	@Override
	protected boolean canRide(Entity entityIn) {
		return true;
	}

	@Override
	protected void addAdditionalSaveData(CompoundNBT compound) {
		compound.putInt("type", blastOrdinal);
		compound.putInt("range", range);
		compound.putBoolean("isItem", isItem);
		if (target != null) {
			compound.putInt("targetX", target.getX());
			compound.putInt("targetY", target.getY());
			compound.putInt("targetZ", target.getZ());
		} else {
			compound.putInt("targetX", 0);
			compound.putInt("targetY", 0);
			compound.putInt("targetZ", 0);
		}

	}

	@Override
	protected void readAdditionalSaveData(CompoundNBT compound) {
		blastOrdinal = compound.getInt("type");
		range = compound.getInt("range");
		isItem = compound.getBoolean("isItem");
		if (blastOrdinal != -1) {
			setBlastType(getBlastType());
		}
		target = new BlockPos(compound.getInt("targetX"), compound.getInt("targetY"), compound.getInt("targetZ"));
	}

	@Override
	public IPacket<?> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}
}
