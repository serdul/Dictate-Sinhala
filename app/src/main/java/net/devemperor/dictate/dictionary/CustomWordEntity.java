package net.devemperor.dictate.dictionary;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "custom_words")
public class CustomWordEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "trigger_word")
    public String triggerWord;

    @ColumnInfo(name = "replacement")
    public String replacement;

    @Nullable
    @ColumnInfo(name = "profession_tag")
    public String professionTag;

    public CustomWordEntity(String triggerWord, String replacement, @Nullable String professionTag) {
        this.triggerWord = triggerWord;
        this.replacement = replacement;
        this.professionTag = professionTag;
    }
}
