package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.LocalizedText;
import com.kidxoxo.talenteditor.domain.MessageDisplay;
import com.kidxoxo.talenteditor.domain.ShowMessageAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class ShowMessageActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "mode", "text", "subtitle", "fadeInTicks", "stayTicks", "fadeOutTicks"
    );

    @Override
    public ResourceLocation id() {
        return ShowMessageAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER, ActionPlacement.ON_ACQUIRE,
                ActionPlacement.ON_SYNTHESIZE, ActionPlacement.ITEM_USE);
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        String rawMode = JsonAccess.string(
                json, "mode", "chat", false, context.source(), context.path(), context.report()
        );
        MessageDisplay mode = MessageDisplay.parse(rawMode).orElseGet(() -> {
            context.report().error("INVALID_MESSAGE_MODE", context.source(),
                    JsonAccess.child(context.path(), "mode"), "Expected chat, action_bar, or title");
            return MessageDisplay.CHAT;
        });
        if (!json.has("text")) {
            context.report().error("REQUIRED_FIELD", context.source(),
                    JsonAccess.child(context.path(), "text"), "Message text is required");
        }
        LocalizedText text = JsonAccess.localizedText(
                json.get("text"), context.source(), JsonAccess.child(context.path(), "text"), context.report()
        );
        Optional<LocalizedText> subtitle = json.has("subtitle")
                ? Optional.of(JsonAccess.localizedText(
                        json.get("subtitle"), context.source(), JsonAccess.child(context.path(), "subtitle"),
                        context.report()
                ))
                : Optional.empty();
        if (subtitle.isPresent() && mode != MessageDisplay.TITLE) {
            context.report().error("SUBTITLE_REQUIRES_TITLE", context.source(),
                    JsonAccess.child(context.path(), "subtitle"),
                    "subtitle is only available when mode is title");
        }
        int fadeIn = JsonAccess.integer(
                json, "fadeInTicks", 10, 0, 1_200, context.source(), context.path(), context.report()
        );
        int stay = JsonAccess.integer(
                json, "stayTicks", 70, 0, 12_000, context.source(), context.path(), context.report()
        );
        int fadeOut = JsonAccess.integer(
                json, "fadeOutTicks", 20, 0, 1_200, context.source(), context.path(), context.report()
        );
        return Optional.of(new ShowMessageAction(mode, text, subtitle, fadeIn, stay, fadeOut));
    }
}
