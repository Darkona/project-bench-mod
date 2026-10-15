package com.darkona.projectbench.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;

public record FeatureCondition(String feature) implements ICondition {
    public static final MapCodec<FeatureCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("feature").forGetter(FeatureCondition::feature)
    ).apply(i, FeatureCondition::new));

    public FeatureCondition(Feature feature) {
        this(feature.key());
    }

    @Override
    public boolean test(IContext context) {
        Feature f = Feature.byKey(feature);
        return f != null && f.configured();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
