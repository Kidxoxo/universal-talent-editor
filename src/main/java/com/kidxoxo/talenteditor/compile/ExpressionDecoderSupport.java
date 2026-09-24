package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kidxoxo.talenteditor.domain.NumericExpression;

final class ExpressionDecoderSupport {
    private ExpressionDecoderSupport() {
    }

    static NumericExpression expression(
            ActionDecodeContext context, JsonObject json, String field, String fallback
    ) {
        JsonElement value = json.get(field);
        String source = fallback;
        if (value == null) {
            context.report().error("REQUIRED_FIELD", context.source(),
                    JsonAccess.child(context.path(), field), "Required field is missing");
        } else if (value instanceof JsonPrimitive primitive
                && (primitive.isString() || primitive.isNumber())) {
            source = primitive.getAsString();
            if (source.isBlank()) {
                context.report().error("BLANK_STRING", context.source(),
                        JsonAccess.child(context.path(), field), "Value must not be blank");
                source = fallback;
            }
        } else {
            context.report().error("EXPECTED_NUMERIC_EXPRESSION", context.source(),
                    JsonAccess.child(context.path(), field), "Expected a numeric value or expression string");
        }
        try {
            return NumericExpression.compile(source);
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_NUMERIC_EXPRESSION", context.source(),
                    JsonAccess.child(context.path(), field), exception.getMessage());
            return NumericExpression.compile("0");
        }
    }
}
