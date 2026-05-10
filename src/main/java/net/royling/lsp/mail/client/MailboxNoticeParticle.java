package net.royling.lsp.mail.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;

public class MailboxNoticeParticle extends SingleQuadParticle {
    public MailboxNoticeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.hasPhysics = false;
        this.lifetime = 18;
        this.quadSize = 0.45F;
        setAlpha(1.0F);
    }

    @Override
    public void tick() {
        if (age++ >= lifetime) {
            remove();
        }
    }

    @Override
    protected Layer getLayer() {
        return Layer.bySprite(sprite);
    }
}
