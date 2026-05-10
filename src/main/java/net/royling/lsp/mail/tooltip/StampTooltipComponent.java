package net.royling.lsp.mail.tooltip;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record StampTooltipComponent(Identifier texture, String foilEffect) implements TooltipComponent {
}
