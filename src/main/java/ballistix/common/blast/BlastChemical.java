package ballistix.common.blast;

import ballistix.common.block.SubtypeBlast;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlastChemical extends Blast {

	public BlastChemical(World world, BlockPos position) {
		super(world, position);
	}

	@Override
	public boolean isInstantaneous() {
		return false;
	}

	@Override
	public void doPreExplode() {
	}

	@Override
	public boolean doExplode(int callCount) {
		if (world.isRemote && world instanceof ClientWorld && callCount % 3 == 0) {
			int radius = 7;
			for (int x = -radius; x <= radius; x++) {
				for (int y = -radius; y <= radius; y++) {
					for (int z = -radius; z <= radius; z++) {
						if (x * x + y * y + z * z < radius * radius && world.rand.nextDouble() < 1 / 20.0) {
							world.addParticle(new RedstoneParticleData(0.7f, 0.8f, 0, 5), position.getX() + x + 0.5 + world.rand.nextDouble() - 1.0, position.getY() + y + 0.5 + world.rand.nextDouble() - 1.0,
									position.getZ() + z + 0.5 + world.rand.nextDouble() - 1.0, 0.0D, 0.0D, 0.0D);
						}
					}
				}
			}
		}
		return callCount > 1200;
	}

	@Override
	public void doPostExplode() {
	}

	@Override
	public SubtypeBlast getBlastType() {
		return SubtypeBlast.chemical;
	}

}
