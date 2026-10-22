package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.api.AdjustabilityListener;
import io.github.safetimamovic.adjustability.api.AnvilEvent;
import io.github.safetimamovic.adjustability.api.SwitchEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Other mods' {@link AdjustabilityListener}s, called in the order they were added. A listener that throws is logged and
 * counts as allowing, so a broken mod can't stop switches or anvils.
 */
public final class Listeners
{
        private static final List<AdjustabilityListener> LISTENERS = new CopyOnWriteArrayList<>();

        private Listeners()
        {
        }

        public static void add(AdjustabilityListener listener)
        {
                LISTENERS.add(listener);
        }

        public static void remove(AdjustabilityListener listener)
        {
                LISTENERS.remove(listener);
        }

        static boolean allowSwitch(SwitchEvent event)
        {
                return allAllow(listener -> listener.allowSwitch(event));
        }

        static void onSwitched(SwitchEvent event)
        {
                for (AdjustabilityListener listener : LISTENERS)
                {
                        try
                        {
                                listener.onSwitched(event);
                        }
                        catch (RuntimeException exception)
                        {
                                Constants.LOG.error("An Adjustability listener failed in onSwitched", exception);
                        }
                }
        }

        static boolean allowAnvilResult(AnvilEvent event)
        {
                return allAllow(listener -> listener.allowAnvilResult(event));
        }

        private static boolean allAllow(Predicate<AdjustabilityListener> check)
        {
                for (AdjustabilityListener listener : LISTENERS)
                {
                        try
                        {
                                if (!check.test(listener))
                                {
                                        return false;
                                }
                        }
                        catch (RuntimeException exception)
                        {
                                Constants.LOG.error("An Adjustability listener failed; treating it as allowing",
                                        exception);
                        }
                }
                return true;
        }
}
