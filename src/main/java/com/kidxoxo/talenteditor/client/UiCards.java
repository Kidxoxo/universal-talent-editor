package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.PlayerTalentState;
import net.minecraft.network.chat.Component;
import java.util.*;

final class UiCards {
    static UiScene.Card talent(PlayerTalentState.TalentView t, boolean selected, boolean effects, List<Component> extra) {
        List<Component> body=new ArrayList<>();
        if(!t.defined())body.add(Component.translatable("screen.talent_editor.book.missing_definition"));
        if(!t.available())body.add(Component.translatable("screen.talent_editor.book.unavailable"));
        t.description().forEach(d->body.add(d.component(0xD0D0D0)));
        if(effects&&t.showGeneratedEffects())for(var effect:t.attributeEffects()) {
            if(!effect.showInDescription())continue;
            var attribute=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(effect.attribute());
            String name=attribute==null?effect.attribute().toString():Component.translatable(attribute.getDescriptionId()).getString();
            String value=effect.formula().isBlank()?String.format(Locale.ROOT,"%+.3f",effect.amount()).replaceFirst("0+$", "").replaceFirst("\\.$", "")
                    :Component.translatable("screen.talent_editor.book.dynamic_effect").getString();
            body.add(Component.literal(name+" "+value+" ").append(Component.translatable("screen.talent_editor.operation."+effect.operation())));
        }
        body.addAll(extra);
        return new UiScene.Card(t.name().component(t.rarityColor()),t.rarityName().component(0xA0A0A0),body,t.icon(),t.rarityColor(),selected);
    }
    static UiScene.Card counter(PlayerTalentState.CounterProgressView c,boolean selected) {
        int color=c.active()?0x55FF55:c.defined()?0xFFAA55:0x808080;
        return new UiScene.Card(c.name().component(color),Component.literal(Long.toString(c.current())),
                List.of(Component.translatable("screen.talent_editor.book.counter_event",Component.translatable("screen.talent_editor.counter.event."+c.event())),
                        Component.translatable("screen.talent_editor.book.counter_status."+(!c.defined()?"missing":c.active()?"active":"inactive"))),Optional.empty(),color,selected);
    }
}
