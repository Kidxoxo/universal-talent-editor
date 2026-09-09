package com.kidxoxo.talenteditor.client;

public final class EditorTextEscapes {
    private EditorTextEscapes() {
    }

    public static String encodeMultiline(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.replace("\\", "\\\\").replace("\n", "\\n");
    }

    public static String decodeMultiline(String value) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current != '\\' || index + 1 >= value.length()) {
                result.append(current);
                continue;
            }
            char next = value.charAt(++index);
            if (next == 'n') result.append('\n');
            else if (next == '\\') result.append('\\');
            else result.append('\\').append(next);
        }
        return result.toString();
    }
}
