package fi.dy.masa.minihud.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ServuxTickData(double mspt,
                             double tps,
                             long sprintTicks,
                             boolean frozen,
                             boolean sprinting,
                             boolean stepping
) {
    public static Codec<ServuxTickData> CODEC = RecordCodecBuilder.create(
            (inst) -> inst.group(
                    PrimitiveCodec.DOUBLE.fieldOf("mspt").forGetter(ServuxTickData::mspt),
                    PrimitiveCodec.DOUBLE.fieldOf("tps").forGetter(ServuxTickData::tps),
                    PrimitiveCodec.LONG.fieldOf("sprintTicks").forGetter(ServuxTickData::sprintTicks),
                    PrimitiveCodec.BOOL.fieldOf("frozen").forGetter(ServuxTickData::frozen),
                    PrimitiveCodec.BOOL.fieldOf("sprinting").forGetter(ServuxTickData::sprinting),
                    PrimitiveCodec.BOOL.fieldOf("stepping").forGetter(ServuxTickData::stepping)
                                ).apply(inst, ServuxTickData::new));
}
