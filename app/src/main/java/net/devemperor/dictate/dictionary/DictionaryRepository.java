package net.devemperor.dictate.dictionary;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DictionaryRepository {

    private final CustomWordDao dao;
    private static final ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<>();

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
     * Patterns are cached for performance.
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
        for (int i = 0; i < words.size(); i++) {
            CustomWordEntity entry = words.get(i);
            String trigger = entry.triggerWord;
            if (trigger == null || trigger.isEmpty()) continue;

            Pattern pattern = patternCache.computeIfAbsent(trigger, t -> Pattern.compile("(?i)\\b" + Pattern.quote(t) + "\\b"));
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                result = matcher.replaceAll(Matcher.quoteReplacement(entry.replacement != null ? entry.replacement : ""));
            }
        }
        return result;
    }
}
