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
                .register(new ModifyNumericStateActionDecoder())
                .register(new ModifyDamageExpressionActionDecoder())
                .register(new AffectHealthExpressionActionDecoder())
                .register(new ModifyAttributeBaseExpressionActionDecoder())
                .register(new TimedAttributeModifierActionDecoder())
                .register(new GrantTalentActionDecoder())
                .register(new RevokeTalentActionDecoder())
                .register(new GiveItemActionDecoder())
                .register(new GiveDrawTokensActionDecoder())
                .register(new ModifyCounterActionDecoder())
                .register(new TakeItemActionDecoder())
                .register(new HudResourceActionDecoder())
                .register(new PlayerRenderActionDecoder())
                .register(new OpenItemSubmissionActionDecoder())
                .register(new PlaySoundActionDecoder())
                .register(new SpawnParticlesActionDecoder())
                .register(new PresentationTransformActionDecoder())
                .register(new ShowMessageActionDecoder())
                .build();
    }
}
