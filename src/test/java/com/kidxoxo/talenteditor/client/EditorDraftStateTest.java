package com.kidxoxo.talenteditor.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorDraftStateTest {
    @Test void staleResponseCannotAcknowledgeAnotherSubmission() {
        var state=new EditorDraftState();state.initialize("old");state.begin(41,"new");
        assertFalse(state.complete(40,true));assertTrue(state.pending());assertTrue(state.dirty("new"));
        assertTrue(state.complete(41,true));assertFalse(state.pending());assertFalse(state.dirty("new"));
        assertFalse(state.complete(41,true));
    }
    @Test void onlySubmittedSnapshotBecomesSaved() {
        var state=new EditorDraftState();state.initialize("old");state.begin(7,"submitted");
        assertThrows(IllegalStateException.class,()->state.begin(8,"later"));
        state.complete(7,true);assertTrue(state.dirty("later"));assertFalse(state.dirty("submitted"));
    }
    @Test void ValidationAndFailurePreserveDirtyStateAcrossReinitialization() {
        var state=new EditorDraftState();state.initialize("old");state.initialize("changed");
        state.begin(2,"changed");state.complete(2,false);
        assertTrue(state.dirty("changed"));assertFalse(state.dirty("old"));
    }
}
