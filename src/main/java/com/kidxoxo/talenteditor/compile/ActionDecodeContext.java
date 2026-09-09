package com.kidxoxo.talenteditor.compile;

import com.kidxoxo.talenteditor.validation.ValidationReport;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ActionDecodeContext(
        ResourceLocation talentId,
        String source,
        String path,
        ActionPlacement placement,
        CompilationEnvironment environment,
        List<String> missingMods,
        ValidationReport.Builder report
) {
    public String defaultModifierKey() {
        String suffix = path.replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase(java.util.Locale.ROOT);
        return talentId.getNamespace() + "." + talentId.getPath().replace('/', '.') + suffix;
    }
}

