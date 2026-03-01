package net.mcreator.trollden.immortal;

import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public class ImmortalData {

    public static UUID trollUUID = null;
    public static Vec3 lastPosition = null;
	public static String lastLevelKey = null;
    public static CompoundTag savedNBT = null;
	public static int lastEntityId = -1;

}