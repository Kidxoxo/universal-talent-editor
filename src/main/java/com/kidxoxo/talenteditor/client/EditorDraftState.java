package com.kidxoxo.talenteditor.client;

import java.util.Objects;

/** Tracks a saved snapshot separately from the exact snapshot currently being submitted. */
final class EditorDraftState {
    private String saved;
    private String submitted;
    private long request;
    void initialize(String snapshot) { if (saved == null) saved = snapshot; }
    boolean dirty(String snapshot) { return saved != null && !Objects.equals(saved, snapshot); }
    boolean pending() { return request != 0; }
    void begin(long id, String snapshot) {
        if (pending() || id == 0) throw new IllegalStateException("A submission is already pending");
        request = id; submitted = snapshot;
    }
    boolean complete(long id, boolean committed) {
        if (!pending() || request != id) return false;
        if (committed) saved = submitted;
        request = 0; submitted = null; return true;
    }
    void saved(String snapshot) { saved = snapshot; }
}
