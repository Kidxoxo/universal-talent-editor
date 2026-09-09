package com.kidxoxo.talenteditor.editor;

public enum DefinitionUndoStatus {
    UNDONE,
    NOT_FOUND,
    ALREADY_UNDONE,
    STALE_TARGET,
    REVISION_CONFLICT,
    BLOCKED_REFERENCES,
    VALIDATION_FAILED,
    IO_FAILED,
    RELOAD_FAILED,
    PERMISSION_DENIED
}
