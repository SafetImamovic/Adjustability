package io.github.safetimamovic.adjustability.gametest;

import com.google.gson.JsonObject;
import io.github.safetimamovic.adjustability.config.CreatorConfig;
import io.github.safetimamovic.adjustability.config.PanelProfiles;
import io.github.safetimamovic.adjustability.config.PanelProfiles.Look;
import io.github.safetimamovic.adjustability.config.PanelProfiles.ProfileException;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import io.github.safetimamovic.adjustability.platform.Services;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Panel profiles and share codes, without a client. Explicit defaults keep existing saved player preferences out
 * of the fixtures.
 */
final class PanelProfileTests
{
        private PanelProfileTests()
        {
        }

        /**
         * Every built-in profile is valid as written; a code gives back the look it was made from; codes and entries
         * that don't fit are refused or skipped, never applied.
         */
        static void profilesAndCodes(GameTestHelper helper)
        {
                try
                {
                        for (String id : PanelProfiles.BUILT_IN)
                        {
                                Look look = PanelProfiles.load(id);
                                helper.assertTrue(look.skipped().isEmpty(), id + " skips " + look.skipped());
                        }
                        helper.assertValueEqual(PanelProfiles.load("magenta").values(), Map.of(),
                                "magenta is the defaults");

                        helper.assertValueEqual(((ConfigValue<?>) CreatorConfig.SPEC.getValues()
                                .get("toggle.allow_none")).getDefault(), true, "None is allowed by default");
                        Map<ConfigValue<?>, Object> defaults = new LinkedHashMap<>();
                        PlayerConfig.LOOK_SETTINGS.forEach(value -> defaults.put(value, value.getDefault()));
                        Map<ConfigValue<?>, Object> look = new LinkedHashMap<>(defaults);
                        look.putAll(PanelProfiles.load("high_contrast").values());
                        String code = PanelProfiles.encode(look);
                        helper.assertTrue(code.startsWith("ADJ-" + Services.PLATFORM.modVersion() + ":"),
                                "a code with this version: " + code);
                        Map<ConfigValue<?>, Object> decoded = new LinkedHashMap<>(defaults);
                        decoded.putAll(PanelProfiles.decode(code).values());
                        helper.assertValueEqual(decoded, look, "the code's complete look");
                        helper.assertValueEqual(PanelProfiles.decode(PanelProfiles.encode(defaults))
                                .values(), Map.of(), "the defaults' code holds nothing");

                        JsonObject json = new JsonObject();
                        json.addProperty("mark_style", "adjustability:toggle");
                        json.addProperty("background_color", "#12345678");
                        json.addProperty("padding", 99);
                        json.addProperty("bracket_color", "not a color");
                        json.addProperty("group_style", "header");
                        json.addProperty("activation", "HOLD");
                        json.addProperty("made_up", true);
                        Look parsed = PanelProfiles.decode(code(json.toString()));
                        helper.assertValueEqual(PanelProfiles.decode(code("0.1.0", json.toString())).values(),
                                parsed.values(), "an older version's code");
                        helper.assertValueEqual(PanelProfiles.decode(code("999.0.0", json.toString())).values(),
                                parsed.values(), "a newer version's code in a format this one reads");
                        helper.assertValueEqual(parsed.skipped(),
                                List.of("padding", "bracket_color", "activation", "made_up"),
                                "out of range, not a color, not a look setting, unknown");
                        helper.assertValueEqual(parsed.values().size(), 3, "the three that fit");
                }
                catch (ProfileException exception)
                {
                        throw new AssertionError(exception.getMessage(), exception);
                }

                for (String bad : List.of("", "hello", "ADJ-", "ADJ-:" + code("{}").split(":", 2)[1], "ADJ-1.0:",
                        "ADJ-1.0:!!!", "ADJ-1.0:AAAA", "ADJ1:" + code("{}").split(":", 2)[1], code("[1, 2]"),
                        code("{\"padding\": "), code(" ".repeat(20_000) + "{}")))
                {
                        try
                        {
                                PanelProfiles.decode(bad);
                                throw new AssertionError("should refuse the code " + bad);
                        }
                        catch (ProfileException expected)
                        {
                                // Refused, as it should be.
                        }
                }
                try
                {
                        PanelProfiles.decode("ADJ-999.0.0:AAAA");
                        throw new AssertionError("should refuse a newer code it can't read");
                }
                catch (ProfileException expected)
                {
                        helper.assertTrue(expected.message().getContents() instanceof TranslatableContents contents
                                && contents.getKey().equals("adjustability.profile.newer_code"),
                                "names the newer version: " + expected.getMessage());
                }
                helper.assertTrue(PanelProfiles.compareVersions("0.10.0", "0.9.1") > 0, "0.10.0 after 0.9.1");
                helper.assertTrue(PanelProfiles.compareVersions("1.0.0-beta.1", "1.0") == 0, "suffixes ignored");
                helper.succeed();
        }

        /** A code for any text, the way {@link PanelProfiles#encode} makes them. */
        private static String code(String json)
        {
                return code(Services.PLATFORM.modVersion(), json);
        }

        /** A code for any text, as {@code version} would make it. */
        private static String code(String version, String json)
        {
                Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
                deflater.setInput(json.getBytes(StandardCharsets.UTF_8));
                deflater.finish();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[512];
                while (!deflater.finished())
                {
                        out.write(buffer, 0, deflater.deflate(buffer));
                }
                deflater.end();
                return "ADJ-" + version + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
        }
}
