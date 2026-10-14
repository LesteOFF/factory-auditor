package dev.nostalgiacraft.auditor.compat.create;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.nostalgiacraft.auditor.block.AuditorBlockEntity;
import dev.nostalgiacraft.auditor.block.AuditorDisplayText;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public final class AuditorDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof AuditorBlockEntity auditor)) {
            return EMPTY;
        }
        AuditorDisplayText text = AuditorDisplayText.of(auditor);
        return List.of(text.value().append(" ").append(text.caption()));
    }
}
