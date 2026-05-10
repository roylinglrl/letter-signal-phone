package net.royling.lsp.registry;

import net.royling.lsp.LetterSignalPhone;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, LetterSignalPhone.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> DEFAULT_RING = SOUNDS.register("default_ring", () ->
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "default_ring")));

    private ModSounds() {
    }
}
