package com.kidxoxo.talenteditor.settings;

/** Server-authoritative behavior which is saved beside the generated data pack. */
public record EditorRuntimeSettings(
        boolean automaticSynthesis,
        SynthesisSoundAudience synthesisSoundAudience,
        SynthesisMessageAudience synthesisMessageAudience,
        SoundOverlapPolicy soundOverlapPolicy
) {
    public static final EditorRuntimeSettings DEFAULT = new EditorRuntimeSettings(
            true, SynthesisSoundAudience.PERSONAL, SynthesisMessageAudience.PERSONAL,
            SoundOverlapPolicy.IMMEDIATE
    );

    public EditorRuntimeSettings {
        synthesisSoundAudience = synthesisSoundAudience == null
                ? SynthesisSoundAudience.PERSONAL : synthesisSoundAudience;
        synthesisMessageAudience = synthesisMessageAudience == null
                ? SynthesisMessageAudience.PERSONAL : synthesisMessageAudience;
        soundOverlapPolicy = soundOverlapPolicy == null
                ? SoundOverlapPolicy.IMMEDIATE : soundOverlapPolicy;
    }

    /** Source-compatible convenience for settings written before stage 54. */
    public EditorRuntimeSettings(boolean automaticSynthesis) {
        this(automaticSynthesis, SynthesisSoundAudience.PERSONAL,
                SynthesisMessageAudience.PERSONAL, SoundOverlapPolicy.IMMEDIATE);
    }

    /** Source-compatible convenience for settings written before stage 55. */
    public EditorRuntimeSettings(
            boolean automaticSynthesis,
            SynthesisSoundAudience synthesisSoundAudience,
            SoundOverlapPolicy soundOverlapPolicy
    ) {
        this(automaticSynthesis, synthesisSoundAudience,
                SynthesisMessageAudience.PERSONAL, soundOverlapPolicy);
    }

    public enum SynthesisMessageAudience {
        PERSONAL("personal"),
        GLOBAL("global");

        private final String serializedName;

        SynthesisMessageAudience(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public SynthesisMessageAudience next() {
            return this == PERSONAL ? GLOBAL : PERSONAL;
        }

        public static SynthesisMessageAudience parse(String value) {
            for (SynthesisMessageAudience candidate : values()) {
                if (candidate.serializedName.equalsIgnoreCase(value)) return candidate;
            }
            throw new IllegalArgumentException("Unknown synthesis message audience: " + value);
        }
    }

    public enum SynthesisSoundAudience {
        PERSONAL("personal"),
        GLOBAL("global");

        private final String serializedName;

        SynthesisSoundAudience(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public SynthesisSoundAudience next() {
            return this == PERSONAL ? GLOBAL : PERSONAL;
        }

        public static SynthesisSoundAudience parse(String value) {
            for (SynthesisSoundAudience candidate : values()) {
                if (candidate.serializedName.equalsIgnoreCase(value)) return candidate;
            }
            throw new IllegalArgumentException("Unknown synthesis sound audience: " + value);
        }
    }

    public enum SoundOverlapPolicy {
        IMMEDIATE("immediate"),
        QUEUE("queue");

        private final String serializedName;

        SoundOverlapPolicy(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public SoundOverlapPolicy next() {
            return this == IMMEDIATE ? QUEUE : IMMEDIATE;
        }

        public static SoundOverlapPolicy parse(String value) {
            for (SoundOverlapPolicy candidate : values()) {
                if (candidate.serializedName.equalsIgnoreCase(value)) return candidate;
            }
            throw new IllegalArgumentException("Unknown sound overlap policy: " + value);
        }
    }
}
