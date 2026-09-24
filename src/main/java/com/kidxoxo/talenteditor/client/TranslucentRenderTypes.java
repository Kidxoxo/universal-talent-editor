package com.kidxoxo.talenteditor.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves the translucent counterpart of an opaque entity {@link RenderType} while keeping the
 * original texture.
 *
 * <p>Vertex alpha alone is not enough: the player body is submitted with
 * {@code RenderType#entityCutoutNoCull}, whose transparency state is {@code NO_TRANSPARENCY}, so the
 * batch is drawn without blending. Substituting the matching {@code entityTranslucent} type makes
 * the render type itself set up blending and translucent sorting.</p>
 *
 * <p>The texture is read reflectively from the render type's composite state because vanilla exposes
 * no accessor. Every lookup is guarded: when anything is unexpected the original type is returned
 * unchanged, so the worst case is "that layer stays opaque" instead of a crash.</p>
 */
public final class TranslucentRenderTypes {
    private static final Map<RenderType, RenderType> CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private TranslucentRenderTypes() {
    }

    public static RenderType translucentVariant(RenderType type) {
        if (type == null) return null;
        if (CACHE.containsKey(type)) return CACHE.get(type);
        RenderType resolved = resolve(type);
        CACHE.put(type, resolved);
        return resolved;
    }

    private static RenderType resolve(RenderType type) {
        try {
            if (type.format() != DefaultVertexFormat.NEW_ENTITY) return type;
            if (type.mode() != VertexFormat.Mode.QUADS) return type;

            String name = nameOf(type);
            if (name != null && (name.contains("translucent") || name.contains("glowing")
                    || name.contains("outline") || name.contains("shadow") || name.contains("eyes"))) {
                return type;
            }

            ResourceLocation texture = textureOf(type);
            if (texture == null) return type;
            return RenderType.entityTranslucent(texture);
        } catch (Throwable ignored) {
            return type;
        }
    }

    private static String nameOf(RenderType type) {
        String value = String.valueOf(type);
        if (value == null || value.indexOf('@') >= 0) return null;
        return value.toLowerCase(Locale.ROOT);
    }

    private static ResourceLocation textureOf(RenderType type) {
        Object state = readFieldByTypeName(type, "CompositeState");
        if (state == null) return null;
        Object textureState = readFieldByTypeName(state, "EmptyTextureStateShard");
        if (textureState == null || textureState.getClass().getName().contains("EmptyTextureStateShard")) {
            return null;
        }
        Object value = readFieldByTypeName(textureState, "java.util.Optional");
        if (value instanceof Optional<?> optional
                && optional.orElse(null) instanceof ResourceLocation location) {
            return location;
        }
        return null;
    }

    private static Object readFieldByTypeName(Object target, String typeName) {
        if (target == null) return null;
        for (Class<?> current = target.getClass(); current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                String fieldType = field.getType().getName();
                if (!fieldType.equals(typeName) && !fieldType.contains(typeName)) continue;
                try {
                    field.setAccessible(true);
                    return field.get(target);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
