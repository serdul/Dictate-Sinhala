package net.devemperor.dictate;

public enum OutputMode {
    SINHALA_SCRIPT,   // raw transcription, no post-processing
    ROMANIZED,        // second API call to romanize into Singlish
    TRANSLATE_ENGLISH // second API call to translate to English
}
