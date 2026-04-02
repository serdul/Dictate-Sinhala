package net.devemperor.dictate.dictionary;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {CustomWordEntity.class}, version = 1, exportSchema = false)
public abstract class WaniDatabase extends RoomDatabase {

    private static volatile WaniDatabase INSTANCE;

    public abstract CustomWordDao customWordDao();

    public static WaniDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (WaniDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            WaniDatabase.class,
                            "wani_dictionary.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
