package com.aeroai;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * NeoForge's config screen caps text boxes at 192 characters and won't edit longer values.
 * This variant lifts that limit so long system prompts can be edited in-game.
 */
public final class LongTextConfigScreen {
    private static final int MAX_TEXT_LENGTH = 32_000;

    public static Screen create(ModContainer container, @Nullable Screen parent) {
        return new ConfigurationScreen(container, parent, Section::new);
    }

    static class Section extends ConfigurationScreen.ConfigurationSectionScreen {
        Section(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title) {
            super(parent, type, modConfig, title);
        }

        Section(Context parentContext, Screen parent, Map<String, Object> valueSpecs, String key,
                Set<? extends UnmodifiableConfig.Entry> entrySet, Component title) {
            super(parentContext, parent, valueSpecs, key, entrySet, title);
        }

        @Override
        protected @Nullable Element createStringValue(String key, Predicate<String> tester, Supplier<String> source, Consumer<String> target) {
            EditBox box = new EditBox(font, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, getTranslationComponent(key));
            box.setEditable(true);
            box.setTooltip(Tooltip.create(getTooltipComponent(key, null)));
            box.setMaxLength(MAX_TEXT_LENGTH);
            box.setValue(source.get());
            box.moveCursorToStart(false);
            box.setResponder(newValue -> {
                if (newValue != null && tester.test(newValue)) {
                    if (!newValue.equals(source.get())) {
                        undoManager.add(v -> {
                            target.accept(v);
                            onChanged(key);
                        }, newValue, v -> {
                            target.accept(v);
                            onChanged(key);
                        }, source.get());
                    }
                    box.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
                    return;
                }
                box.setTextColor(0xFFFF0000);
            });
            return new Element(getTranslationComponent(key), getTooltipComponent(key, null), box);
        }

        // Sub-sections must also use this class, otherwise they fall back to the capped text boxes
        @Override
        protected @Nullable Element createSection(String key, UnmodifiableConfig subconfig, UnmodifiableConfig subsection) {
            Element base = super.createSection(key, subconfig, subsection);
            if (base == null || !(base.widget() instanceof Button original)) return base;
            Button button = Button.builder(original.getMessage(),
                            b -> minecraft.setScreen(sectionCache.computeIfAbsent(key,
                                    k -> new Section(context, this, subconfig.valueMap(), key, subsection.entrySet(),
                                            Component.translatable(getTranslationKey(key))).rebuild())))
                    .tooltip(Tooltip.create(getTooltipComponent(key, null)))
                    .width(Button.DEFAULT_WIDTH)
                    .build();
            return new Element(base.name(), base.tooltip(), button, false);
        }
    }

    private LongTextConfigScreen() {}
}
