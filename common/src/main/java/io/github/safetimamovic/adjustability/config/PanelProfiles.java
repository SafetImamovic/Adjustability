package io.github.safetimamovic.adjustability.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.platform.Services;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Panel profiles: named looks for the Adjust panel, and share codes that carry one. A profile is a set of look
 * settings ({@link PlayerConfig#LOOK_SETTINGS}) by their keys in the file; settings it doesn't name take their
 * defaults. Applying one copies its values into the player file, nothing more: there's no link to the profile
 * afterwards, so the player file stays the one place the look lives.
 *
 * <p>Profiles are Adjustability's built-in ones ({@link #BUILT_IN}) and JSON files in
 * {@code config/adjustability/panel_profiles}, which modpacks ship and {@code /adjustability panel save} writes:
 *
 * <pre>{@code
 * {
 *     "mark_style": "adjustability:toggle",
 *     "background_color": "#F0000000"
 * }
 * }</pre>
 *
 * <p>A share code is {@code ADJ1:} and the settings that differ from the defaults, as compact JSON, deflated and in
 * URL-safe Base64. Profiles and codes are read the same way: every value is checked as the config file checks it,
 * and keys this version doesn't know or values it can't use are skipped and reported, never fatal. A code can't hold
 * more than the panel's look.
 */
public final class PanelProfiles
{
        /** Adjustability's own profiles, in the order they're offered; their names are taken. */
        public static final List<String> BUILT_IN = List.of("magenta", "vanilla", "minimal", "high_contrast");

        /** What a profile file's name (without {@code .json}) may be. */
        public static final String ID_PATTERN = "[a-z0-9_.-]+";

        private static final String CODE_PREFIX = "ADJ1:";
        /** A code's JSON may not inflate past this; real ones are a few hundred bytes. */
        private static final int MAX_CODE_JSON = 16 * 1024;

        private static final Map<String, Map<String, Object>> BUILT_IN_VALUES = Map.of(
                "magenta", Map.of(),
                // Vanilla's tooltip colors.
                "vanilla", Map.ofEntries(
                        Map.entry("background_color", "#F0100010"),
                        Map.entry("border_top_color", "#505000FF"),
                        Map.entry("border_bottom_color", "#5028007F"),
                        Map.entry("show_brackets", false),
                        Map.entry("highlight_color", "#30FFFFFF"),
                        Map.entry("active_color", "#FFFFFFFF"),
                        Map.entry("separator_color", "#305000FF"),
                        Map.entry("mark_style", "adjustability:radio")),
                "minimal", Map.ofEntries(
                        Map.entry("background_color", "#D0000000"),
                        Map.entry("border_style", "NONE"),
                        Map.entry("show_brackets", false),
                        Map.entry("highlight_color", "#30FFFFFF"),
                        Map.entry("active_color", "#FFFFFFFF"),
                        Map.entry("dormant_mark_color", "#FF777777"),
                        Map.entry("dormant_text_color", "#FF999999"),
                        Map.entry("mark_style", "adjustability:square"),
                        Map.entry("group_style", "GAP"),
                        Map.entry("show_cost", false),
                        Map.entry("text_shadow", false),
                        Map.entry("padding", 4)),
                "high_contrast", Map.ofEntries(
                        Map.entry("background_color", "#FF000000"),
                        Map.entry("border_style", "SOLID"),
                        Map.entry("border_top_color", "#FFFFFFFF"),
                        Map.entry("show_brackets", false),
                        Map.entry("highlight_color", "#80FFFF00"),
                        Map.entry("active_color", "#FFFFFF00"),
                        Map.entry("dormant_mark_color", "#FFFFFFFF"),
                        Map.entry("title_color", "#FFFFFF00"),
                        Map.entry("active_text_color", "#FFFFFF00"),
                        Map.entry("dormant_text_color", "#FFFFFFFF"),
                        Map.entry("footer_text_color", "#FFFFFFFF"),
                        Map.entry("separator_color", "#FFFFFFFF"),
                        Map.entry("mark_style", "adjustability:checkbox"),
                        Map.entry("group_style", "HEADER"),
                        Map.entry("row_height", 13)));

        private PanelProfiles()
        {
        }

        /** Where profile files live: {@code config/adjustability/panel_profiles}. */
        public static Path folder()
        {
                return Services.PLATFORM.configDirectory().resolve(Constants.MOD_ID).resolve("panel_profiles");
        }

        /** {@code file} from the game folder, like {@code config/adjustability/panel_profiles/mine.json}. */
        public static String display(Path file)
        {
                Path game = Services.PLATFORM.configDirectory().toAbsolutePath().getParent();
                Path absolute = file.toAbsolutePath();
                return (game != null && absolute.startsWith(game) ? game.relativize(absolute) : absolute).toString()
                        .replace('\\', '/');
        }

        public static boolean isBuiltIn(String id)
        {
                return BUILT_IN.contains(id);
        }

        /** Every profile's id: the built-in ones, then the files' in alphabetical order. */
        public static List<String> ids()
        {
                List<String> ids = new ArrayList<>(BUILT_IN);
                ids.addAll(files());
                return ids;
        }

        /** The profile files' ids; a file named like a built-in profile is ignored. */
        private static List<String> files()
        {
                Path folder = folder();
                if (!Files.isDirectory(folder))
                {
                        return List.of();
                }
                try (Stream<Path> files = Files.list(folder))
                {
                        return files.map(file -> file.getFileName().toString())
                                .filter(name -> name.endsWith(".json"))
                                .map(name -> name.substring(0, name.length() - ".json".length()))
                                .filter(id -> id.matches(ID_PATTERN) && !isBuiltIn(id))
                                .sorted()
                                .toList();
                }
                catch (IOException exception)
                {
                        Constants.LOG.warn("Couldn't list the panel profiles in {}", folder, exception);
                        return List.of();
                }
        }

        /** A profile's look, by id. */
        public static Look load(String id) throws ProfileException
        {
                Map<String, Object> builtIn = BUILT_IN_VALUES.get(id);
                if (builtIn != null)
                {
                        JsonObject json = new JsonObject();
                        builtIn.forEach((key, value) -> json.add(key, toJson(value)));
                        return parse(json);
                }
                Path file = folder().resolve(id + ".json");
                if (!id.matches(ID_PATTERN) || !Files.isRegularFile(file))
                {
                        throw new ProfileException(Component.translatable("adjustability.profile.not_found", id));
                }
                try
                {
                        return parse(object(Files.readString(file, StandardCharsets.UTF_8)));
                }
                catch (IOException | JsonParseException | IllegalStateException exception)
                {
                        throw new ProfileException(Component.translatable("adjustability.profile.unreadable",
                                display(file), String.valueOf(exception.getMessage())));
                }
        }

        /**
         * Writes {@code look} (every look setting) as the profile file {@code id}, replacing one that's there.
         *
         * @return the file
         */
        public static Path save(String id, Map<ConfigValue<?>, Object> look) throws ProfileException
        {
                if (!id.matches(ID_PATTERN))
                {
                        throw new ProfileException(Component.translatable("adjustability.profile.bad_name", id));
                }
                if (isBuiltIn(id))
                {
                        throw new ProfileException(Component.translatable("adjustability.profile.built_in_name", id));
                }
                Path file = folder().resolve(id + ".json");
                try
                {
                        Files.createDirectories(file.getParent());
                        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create()
                                .toJson(json(look, false)) + "\n", StandardCharsets.UTF_8);
                        return file;
                }
                catch (IOException exception)
                {
                        throw new ProfileException(Component.translatable("adjustability.profile.cant_save",
                                display(file), String.valueOf(exception.getMessage())));
                }
        }

        /** The share code for {@code look}: only the settings that differ from the defaults. */
        public static String encode(Map<ConfigValue<?>, Object> look)
        {
                byte[] json = json(look, true).toString().getBytes(StandardCharsets.UTF_8);
                Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
                deflater.setInput(json);
                deflater.finish();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[512];
                while (!deflater.finished())
                {
                        out.write(buffer, 0, deflater.deflate(buffer));
                }
                deflater.end();
                return CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
        }

        /** The look a share code carries. */
        public static Look decode(String code) throws ProfileException
        {
                String trimmed = code.trim();
                if (!trimmed.startsWith(CODE_PREFIX))
                {
                        throw badCode();
                }
                Inflater inflater = new Inflater(true);
                try
                {
                        inflater.setInput(Base64.getUrlDecoder().decode(trimmed.substring(CODE_PREFIX.length())));
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        byte[] buffer = new byte[512];
                        while (!inflater.finished())
                        {
                                int read = inflater.inflate(buffer);
                                if (read == 0 && (inflater.needsInput() || inflater.needsDictionary()))
                                {
                                        throw badCode();
                                }
                                out.write(buffer, 0, read);
                                if (out.size() > MAX_CODE_JSON)
                                {
                                        throw badCode();
                                }
                        }
                        return parse(object(out.toString(StandardCharsets.UTF_8)));
                }
                catch (IllegalArgumentException | DataFormatException | JsonParseException
                       | IllegalStateException exception)
                {
                        throw badCode();
                }
                finally
                {
                        inflater.end();
                }
        }

        private static ProfileException badCode()
        {
                return new ProfileException(Component.translatable("adjustability.profile.bad_code"));
        }

        private static JsonObject object(String json)
        {
                JsonElement element = JsonParser.parseString(json);
                if (!element.isJsonObject())
                {
                        throw new JsonParseException("not a JSON object");
                }
                return element.getAsJsonObject();
        }

        /** Checks every entry against its setting; what doesn't fit is skipped. */
        static Look parse(JsonObject json)
        {
                Map<ConfigValue<?>, Object> values = new LinkedHashMap<>();
                List<String> skipped = new ArrayList<>();
                for (Map.Entry<String, JsonElement> entry : json.entrySet())
                {
                        Optional<ConfigValue<?>> setting = PlayerConfig.lookSetting(entry.getKey());
                        Optional<Object> value = setting.flatMap(found -> value(found, entry.getValue()));
                        if (value.isPresent())
                        {
                                values.put(setting.get(), value.get());
                        }
                        else
                        {
                                skipped.add(entry.getKey());
                        }
                }
                return new Look(values, skipped);
        }

        /** {@code element} as a value {@code setting} takes, if it is one. */
        private static Optional<Object> value(ConfigValue<?> setting, JsonElement element)
        {
                if (!(element instanceof JsonPrimitive primitive))
                {
                        return Optional.empty();
                }
                Object initial = setting.getDefault();
                Object value = null;
                if (initial instanceof Boolean && primitive.isBoolean())
                {
                        value = primitive.getAsBoolean();
                }
                else if (initial instanceof Integer && primitive.isNumber())
                {
                        double number = primitive.getAsDouble();
                        boolean whole = number == Math.rint(number) && Math.abs(number) <= Integer.MAX_VALUE;
                        value = whole ? (int) number : null;
                }
                else if (initial instanceof Enum<?> constant && primitive.isString())
                {
                        value = Arrays.stream(constant.getDeclaringClass().getEnumConstants())
                                .filter(candidate -> candidate.name().equalsIgnoreCase(primitive.getAsString()))
                                .findFirst().orElse(null);
                }
                else if (initial instanceof String && primitive.isString())
                {
                        value = PlayerConfig.isColor(setting) ? PlayerConfig.formatColor(primitive.getAsString())
                                : primitive.getAsString();
                }
                return value != null && setting.getSpec().test(value) ? Optional.of(value) : Optional.empty();
        }

        /** {@code look} as JSON, in file order; with {@code changedOnly}, without the settings at their default. */
        private static JsonObject json(Map<ConfigValue<?>, Object> look, boolean changedOnly)
        {
                JsonObject json = new JsonObject();
                for (ConfigValue<?> setting : PlayerConfig.LOOK_SETTINGS)
                {
                        Object value = look.getOrDefault(setting, setting.getDefault());
                        if (!changedOnly || !value.equals(setting.getDefault()))
                        {
                                json.add(PlayerConfig.name(setting), toJson(value));
                        }
                }
                return json;
        }

        private static JsonElement toJson(Object value)
        {
                return switch (value)
                {
                        case Boolean bool -> new JsonPrimitive(bool);
                        case Number number -> new JsonPrimitive(number);
                        case Enum<?> constant -> new JsonPrimitive(constant.name());
                        default -> new JsonPrimitive(String.valueOf(value));
                };
        }

        /**
         * A look read from a profile or a code.
         *
         * @param values  the settings it sets, each checked; the others take their defaults
         * @param skipped the keys it had that aren't settings, or whose values the setting can't take
         */
        public record Look(Map<ConfigValue<?>, Object> values, List<String> skipped)
        {
        }

        /** A profile or code that can't be used, with a message for the player. */
        public static final class ProfileException extends Exception
        {
                private final transient Component message;

                ProfileException(Component message)
                {
                        super(message.getString());
                        this.message = message;
                }

                public Component message()
                {
                        return this.message;
                }
        }
}
