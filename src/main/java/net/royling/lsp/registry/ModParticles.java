package net.royling.lsp.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.royling.lsp.LetterSignalPhone;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, LetterSignalPhone.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MAILBOX_NOTICE =
            PARTICLES.register("mailbox_notice", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
