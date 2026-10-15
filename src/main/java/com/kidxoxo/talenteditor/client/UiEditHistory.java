package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.UiDocument;
import java.util.ArrayDeque;

/** A drag is one transaction, regardless of how many pointer events it produces. */
final class UiEditHistory {
    private UiDocument value;
    private final ArrayDeque<UiDocument> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    UiEditHistory(UiDocument value) { this.value=value; }
    UiDocument value() { return value; }
    boolean canUndo() { return !undo.isEmpty(); }
    boolean canRedo() { return !redo.isEmpty(); }
    void commit(UiDocument next) {
        if(value.equals(next))return;
        undo.addLast(value);if(undo.size()>64)undo.removeFirst();redo.clear();value=next;
    }
    void undo() { if(!undo.isEmpty()){redo.addLast(value);value=undo.removeLast();} }
    void redo() { if(!redo.isEmpty()){undo.addLast(value);value=redo.removeLast();} }
    static int snap(double value,int grid) { return (int)Math.round(value/grid)*grid; }
}
