package io.github.safetimamovic.adjustability.custom_data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class CustomData extends SavedData
{
	@Override
	public CompoundTag save (CompoundTag pTag, HolderLookup.Provider pRegistries)
	{
		return null;
	}

//	@Override
//	public CompoundTag save(CompoundTag tag) {
//		//save to tag
//		int size = beaconPositions.size();
//		tag.putInt("size", size);
//		for(int i = 0; i < size; i++) {
//			tag.put("beacon_pos_" + i, NbtUtils.writeBlockPos(beaconPositions.get(i)));
//		}
//		return tag;
//	}
}
