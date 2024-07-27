package io.github.safetimamovic.adjustability.custom_data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.github.safetimamovic.adjustability.Adjustability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility Class that handles writing data to different JSON files using Gson.
 */
public class PersistData
{

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path ENCHANTMENT_DATA_PATH = Paths.get("config/" + Adjustability.MODID + "/enchantment_data.json");
	private static final Path MUTUALLY_EXCLUSIVE_DATA_PATH = Paths.get("config/" + Adjustability.MODID + "/mutually_exclusive_data.json");

	public static void saveStringArray (ArrayList<String> data)
	{
		JsonArray jsonArray = new JsonArray();
		for ( String str : data )
		{
			jsonArray.add(str);
		}

		try ( FileWriter writer = new FileWriter(ENCHANTMENT_DATA_PATH.toFile()) )
		{
			GSON.toJson(jsonArray, writer);
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public static String[] loadStringArray ()
	{
		if ( !Files.exists(ENCHANTMENT_DATA_PATH) )
		{
			return new String[0];
		}

		try ( FileReader reader = new FileReader(ENCHANTMENT_DATA_PATH.toFile()) )
		{
			JsonArray jsonArray = GSON.fromJson(reader, JsonArray.class);
			String[] data = new String[jsonArray.size()];
			for ( int i = 0; i < jsonArray.size(); i++ )
			{
				data[i] = jsonArray.get(i).getAsString();
			}
			return data;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		return new String[0];
	}

	/**
	 * Static method that handles initialization of the Adjustability mod data files.
	 * If the JSON file with the provided name doesn't exist
	 * then it creates it.
	 * If the <strong>adjustability/</strong> directory doesn't exist
	 * then it creates it.
	 */
	public static void initialize ()
	{
		try
		{
			if ( !Files.exists(ENCHANTMENT_DATA_PATH.getParent()) )
			{
				Files.createDirectories(ENCHANTMENT_DATA_PATH.getParent());
			}
			if ( !Files.exists(ENCHANTMENT_DATA_PATH) )
			{
				Files.createFile(ENCHANTMENT_DATA_PATH);
			}
			initializeMutuallyExclusiveData();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	private static void initializeMutuallyExclusiveData ()
	{
		try
		{
			if ( !Files.exists(MUTUALLY_EXCLUSIVE_DATA_PATH) )
			{
				Files.createFile(MUTUALLY_EXCLUSIVE_DATA_PATH);

				List<List<String>> mutuallyExclusiveData = Arrays.asList(
					Arrays.asList("minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection", "minecraft:projectile_protection"),
					Arrays.asList("minecraft:sharpness", "minecraft:smite", "minecraft:bane_of_arthropods"),
					Arrays.asList("minecraft:depth_strider", "minecraft:frost_walker"),
					Arrays.asList("minecraft:silk_touch", "minecraft:fortune"),
					Arrays.asList("minecraft:multishot", "minecraft:piercing"),
					Arrays.asList("minecraft:channeling", "minecraft:riptide")
				);

				JsonArray jsonArray = new JsonArray();
				for ( List<String> group : mutuallyExclusiveData )
				{
					JsonArray groupArray = new JsonArray();
					for ( String enchantment : group )
					{
						groupArray.add(enchantment);
					}
					jsonArray.add(groupArray);
				}

				try ( FileWriter writer = new FileWriter(MUTUALLY_EXCLUSIVE_DATA_PATH.toFile()) )
				{
					GSON.toJson(jsonArray, writer);
				}
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public static List<List<Enchantment>> loadMutuallyExclusiveData ()
	{
		if ( !Files.exists(MUTUALLY_EXCLUSIVE_DATA_PATH) )
		{
			return new ArrayList<>();
		}

		try ( FileReader reader = new FileReader(MUTUALLY_EXCLUSIVE_DATA_PATH.toFile()) )
		{
			JsonArray jsonArray = GSON.fromJson(reader, JsonArray.class);
			List<List<Enchantment>> data = new ArrayList<>();
			for ( JsonElement element : jsonArray )
			{
				JsonArray groupArray = element.getAsJsonArray();
				List<Enchantment> group = new ArrayList<>();
				for ( JsonElement enchantment : groupArray )
				{
					group.add(ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.tryParse(enchantment.getAsString())));
				}
				data.add(group);
			}
			return data;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
}
