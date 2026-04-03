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
     * Patterns are compiled once per call for performance.
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

        // Pre-compile all patterns before processing
        Pattern[] patterns = new Pattern[words.size()];
        for (int i = 0; i < words.size(); i++) {
            String trigger = words.get(i).triggerWord;
            if (trigger != null && !trigger.isEmpty()) {
                patterns[i] = Pattern.compile("(?i)\\b" + Pattern.quote(trigger) + "\\b");
            }
        }

        String result = text;
        for (int i = 0; i < words.size(); i++) {
            if (patterns[i] == null) continue;
            CustomWordEntity entry = words.get(i);
            Matcher matcher = patterns[i].matcher(result);
            if (matcher.find()) {
                result = matcher.replaceAll(Matcher.quoteReplacement(entry.replacement != null ? entry.replacement : ""));
            }
        }
        return result;
    }
}
