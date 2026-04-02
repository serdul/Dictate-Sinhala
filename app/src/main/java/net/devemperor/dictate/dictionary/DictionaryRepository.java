package net.devemperor.dictate.dictionary;

import android.content.Context;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DictionaryRepository {

    private final CustomWordDao dao;

    public DictionaryRepository(Context context) {
        this.dao = WaniDatabase.getInstance(context).customWordDao();
    }

    public List<CustomWordEntity> getAll() {
        return dao.getAll();
    }

    public List<CustomWordEntity> getForProfession(String professionTag) {
        return dao.getForProfession(professionTag);
    }

    public void insert(CustomWordEntity word) {
        dao.insert(word);
    }

    public void update(CustomWordEntity word) {
        dao.update(word);
    }

    public void delete(CustomWordEntity word) {
        dao.delete(word);
    }

    /**
     * Apply dictionary replacements to the given text.
     * Matches are case-insensitive and whole-word.
     *
     * @param text          text to process
     * @param professionTag current profession tag, or null for general
     * @return text with replacements applied
     */
    public String applyDictionary(String text, String professionTag) {
        if (text == null || text.isEmpty()) return text;

        List<CustomWordEntity> words;
        if (professionTag != null && !professionTag.isEmpty() && !professionTag.equals("general")) {
            words = dao.getForProfession(professionTag);
        } else {
            words = dao.getAll();
        }

        if (words == null || words.isEmpty()) return text;

        String result = text;
        for (CustomWordEntity entry : words) {
            if (entry.triggerWord == null || entry.triggerWord.isEmpty()) continue;
            String trigger = Pattern.quote(entry.triggerWord);
            Pattern pattern = Pattern.compile("(?i)\\b" + trigger + "\\b");
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                result = matcher.replaceAll(Matcher.quoteReplacement(entry.replacement != null ? entry.replacement : ""));
            }
        }
        return result;
    }
}
