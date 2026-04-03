package net.devemperor.dictate.dictionary;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CustomWordDao {

    @Insert
    void insert(CustomWordEntity word);

    @Update
    void update(CustomWordEntity word);

    @Delete
    void delete(CustomWordEntity word);

    @Query("SELECT * FROM custom_words ORDER BY trigger_word ASC")
    List<CustomWordEntity> getAll();

    @Query("SELECT * FROM custom_words WHERE profession_tag IS NULL OR profession_tag = :tag ORDER BY trigger_word ASC")
    List<CustomWordEntity> getForProfession(String tag);
}
