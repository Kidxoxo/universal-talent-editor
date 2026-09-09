package com.kidxoxo.talenteditor.compile;

public final class BuiltinActionTypes {
    private BuiltinActionTypes() {
    }

    public static ActionTypeRegistry create() {
        return ActionTypeRegistry.builder()
                .register(new AttributeModifierActionDecoder())
                .register(new DynamicAttributeModifierActionDecoder())
                .register(new CreativeFlightActionDecoder())
                .register(new AddEffectActionDecoder())
                .register(new RemoveEffectActionDecoder())
                .register(new EffectImmunityActionDecoder())
                .register(new DrawRefreshBonusActionDecoder())
                .register(new QueueDrawConstraintActionDecoder())
                .register(new ModifyDamageActionDecoder())
                .register(new CancelDamageActionDecoder())
                .register(new ModifyEntityAttributeActionDecoder())
                .register(new DamageEntityActionDecoder())
                .register(new HealEntityActionDecoder())
                .register(new PlaySoundActionDecoder())
                .register(new ShowMessageActionDecoder())
                .build();
    }
}
