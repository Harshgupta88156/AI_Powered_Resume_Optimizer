package com.ai_resume.resume_service.entity;

/** How a job description entered the system. Replaces the old, never-assigned {@code textInput} flag. */
public enum JobDescriptionSource {
    /** Uploaded as a PDF / DOC / DOCX file. */
    FILE,
    /** Pasted as plain text. */
    TEXT
}

