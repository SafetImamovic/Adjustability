package io.github.safetimamovic.adjustability.gametest;

import com.google.gson.JsonObject;
import io.github.safetimamovic.adjustability.config.PanelProfiles;
import io.github.safetimamovic.adjustability.config.PanelProfiles.Look;
import io.github.safetimamovic.adjustability.config.PanelProfiles.ProfileException;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Panel profiles and share codes, without a client: the player file isn't loaded on a server, so every setting reads
 * as its default, which is all these need.
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

                        Map<ConfigValue<?>, Object> look = new LinkedHashMap<>(PlayerConfig.currentLook());
                        look.putAll(PanelProfiles.load("high_contrast").values());
                        String code = PanelProfiles.encode(look);
                        helper.assertTrue(code.startsWith("ADJ1:"), "a code: " + code);
                        helper.assertValueEqual(PanelProfiles.decode(code).values(),
                                PanelProfiles.load("high_contrast").values(), "the code's look");
                        helper.assertValueEqual(PanelProfiles.decode(PanelProfiles.encode(PlayerConfig.currentLook()))
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
                        helper.assertValueEqual(parsed.skipped(),
                                List.of("padding", "bracket_color", "activation", "made_up"),
                                "out of range, not a color, not a look setting, unknown");
                        helper.assertValueEqual(parsed.values().size(), 3, "the three that fit");
                }
                catch (ProfileException exception)
                {
                        throw new AssertionError(exception.getMessage(), exception);
                }

                for (String bad : List.of("", "hello", "ADJ1:", "ADJ1:!!!", "ADJ1:AAAA", "ADJ2:" + "x".repeat(8),
                        code("[1, 2]"), code("{\"padding\": "), code(" ".repeat(20_000) + "{}")))
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
                helper.succeed();
        }

        /** A code for any text, the way {@link PanelProfiles#encode} makes them. */
        private static String code(String json)
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
                return "ADJ1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
        }
}
