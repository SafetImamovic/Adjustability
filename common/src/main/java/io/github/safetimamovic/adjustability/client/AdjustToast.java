package io.github.safetimamovic.adjustability.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
//? if >=26.1 {
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
//?} else
/*import net.minecraft.client.gui.components.toasts.ToastComponent;*/
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The tutorial's book icon and wrapped text on vanilla's tutorial background. */
public final class AdjustToast implements Toast
{
        private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("toast/tutorial");
        private static final ItemStack ICON = new ItemStack(Items.ENCHANTED_BOOK);
        private final List<FormattedCharSequence> lines = new ArrayList<>();
        private final boolean timed;
        private Visibility visibility = Visibility.SHOW;

        AdjustToast(Font font, Component title, Component description, boolean timed)
        {
                this.lines.addAll(font.split(title.copy().withStyle(ChatFormatting.DARK_PURPLE), 126));
                this.lines.addAll(font.split(description, 126));
                this.timed = timed;
        }

        //? if >=26.1 {
        @Override
        public Visibility getWantedVisibility()
        {
                return this.visibility;
        }

        @Override
        public void update(ToastManager manager, long time)
        {
                this.expire(time, manager.getNotificationDisplayTimeMultiplier());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long time)
        {
                this.draw(graphics, font);
        }
        //?} else {
        /*@Override
        public Visibility render(GuiGraphicsExtractor graphics, ToastComponent manager, long time)
        {
                this.expire(time, manager.getNotificationDisplayTimeMultiplier());
                this.draw(graphics, manager.getMinecraft().font);
                return this.visibility;
        }
        *///?}

        private void expire(long time, double multiplier)
        {
                if (this.timed && time >= 8000 * multiplier)
                {
                        this.hide();
                }
        }

        @Override
        public int height()
        {
                return 10 + Math.max(2, this.lines.size()) * 11;
        }

        private void draw(GuiGraphicsExtractor graphics, Font font)
        {
                //? if >=26.1 {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, this.width(), this.height());
                graphics.item(ICON, 7, (this.height() - 16) / 2);
                //?} else {
                /*graphics.blitSprite(BACKGROUND, 0, 0, this.width(), this.height());
                graphics.renderItem(ICON, 7, (this.height() - 16) / 2);
                *///?}
                for (int i = 0; i < this.lines.size(); i++)
                {
                        graphics.text(font, this.lines.get(i), 30, 7 + i * 11, 0xFF000000, false);
                }
        }

        public void hide()
        {
                this.visibility = Visibility.HIDE;
        }

        static void add(Minecraft minecraft, AdjustToast toast)
        {
                //? if >=26.1 {
                minecraft.gui.toastManager().addToast(toast);
                //?} else
                /*minecraft.getToasts().addToast(toast);*/
        }
}
