package io.github.safetimamovic.adjustability.custom_data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.github.safetimamovic.adjustability.Adjustability;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Class that handles writing data to a JSON file.
 */
public class PersistData
{

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path DATA_PATH = Paths.get("config/" + Adjustability.MODID + "/string_data.json");

	// Method to save an array of strings to JSON
	public static void saveStringArray (ArrayList<String> data)
	{
		JsonArray jsonArray = new JsonArray();
		for ( String str : data )
		{
			jsonArray.add(str);
		}

		try ( FileWriter writer = new FileWriter(DATA_PATH.toFile()) )
		{
			GSON.toJson(jsonArray, writer);
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	// Method to load an array of strings from JSON
	public static String[] loadStringArray ()
	{
		if ( !Files.exists(DATA_PATH) )
		{
			return new String[0];
		}

		try ( FileReader reader = new FileReader(DATA_PATH.toFile()) )
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
			if ( !Files.exists(DATA_PATH.getParent()) )
			{
				Files.createDirectories(DATA_PATH.getParent());
			}
			if ( !Files.exists(DATA_PATH) )
			{
				Files.createFile(DATA_PATH);
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}