
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.trollden.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.ForgeSpawnEggItem;

import net.minecraft.world.item.Item;

import net.mcreator.trollden.TrolldenMod;

public class TrolldenModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, TrolldenMod.MODID);
	public static final RegistryObject<Item> TROLLDEN_SPAWN_EGG = REGISTRY.register("trollden_spawn_egg", () -> new ForgeSpawnEggItem(TrolldenModEntities.TROLLDEN, -16764109, -16724788, new Item.Properties()));
	// Start of user code block custom items
	// End of user code block custom items
}
