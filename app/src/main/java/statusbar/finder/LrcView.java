package statusbar.finder;

import static statusbar.finder.misc.Constants.PREFERENCE_KEY_REQUIRE_TRANSLATE;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LrcView extends FragmentActivity {
    private SharedPreferences offsetPreferences;
    private SharedPreferences translationStatusReferences;
    @SuppressLint("SetTextI18n") // Just Test
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lrcview);

        offsetPreferences = getSharedPreferences("offset", MODE_PRIVATE);
        translationStatusReferences = getSharedPreferences("translationstatus", MODE_PRIVATE);

        TextView lrc = findViewById(R.id.lrc);
        TextView offset = findViewById(R.id.offset);
        TextView tran = findViewById(R.id.tran);
        if (MusicListenerService.instance.getLyric() == null)
            lrc.setText("none");
        else {
            lrc.setText(format(MusicListenerService.instance.getLyric().sentenceList.toString()));
            offset.setText("offset: " + ((MusicListenerService.instance.getLyric().offset > 0) ? -MusicListenerService.instance.getLyric().offset : Math.abs(MusicListenerService.instance.getLyric().offset)));
            if (MusicListenerService.instance.getLyric().translatedSentenceList != null && !format(MusicListenerService.instance.getLyric().translatedSentenceList.toString()).isEmpty()) {
                if (PreferenceManager.getDefaultSharedPreferences(MusicListenerService.instance).getBoolean(PREFERENCE_KEY_REQUIRE_TRANSLATE, false))
                    lrc.setText(mergeStrings(MusicListenerService.instance.getLyric().sentenceList.toString(), MusicListenerService.instance.getLyric().translatedSentenceList.toString()));
                else
                    tran.setVisibility(View.VISIBLE);
            }
        }

        if (translationStatusReferences.getBoolean(MusicListenerService.instance.musicInfo, false) && !PreferenceManager.getDefaultSharedPreferences(MusicListenerService.instance).getBoolean(PREFERENCE_KEY_REQUIRE_TRANSLATE, false)) {
            tran.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.teal_700));
            lrc.setText(format(MusicListenerService.instance.getLyric().translatedSentenceList.toString()));
        }
        tran.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tran.getCurrentTextColor() == ContextCompat.getColor(getApplicationContext(), R.color.teal_700)) {
                    tran.setTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.tab_indicator_text));
                    translationStatusReferences.edit().remove(MusicListenerService.instance.musicInfo).apply();
                    lrc.setText(format(MusicListenerService.instance.getLyric().sentenceList.toString()));
                } else {
                    tran.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.teal_700));
                    translationStatusReferences.edit().putBoolean(MusicListenerService.instance.musicInfo, true).apply();
                    lrc.setText(format(MusicListenerService.instance.getLyric().translatedSentenceList.toString()));
                }
                MusicListenerService.instance.sync();
            }
        });

        Button plus =findViewById(R.id.plus);
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MusicListenerService.instance.getLyric() != null) {
                    MusicListenerService.instance.getLyric().offset = (MusicListenerService.instance.getLyric().offset - 100);
                    offset.setText("offset: " + ((MusicListenerService.instance.getLyric().offset > 0) ? -MusicListenerService.instance.getLyric().offset : Math.abs(MusicListenerService.instance.getLyric().offset)));
                    writeOffset();
                }
            }
        });

        Button minus =findViewById(R.id.minus);
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MusicListenerService.instance.getLyric() != null) {
                    MusicListenerService.instance.getLyric().offset = (MusicListenerService.instance.getLyric().offset + 100);
                    offset.setText("offset: " + ((MusicListenerService.instance.getLyric().offset > 0) ? -MusicListenerService.instance.getLyric().offset : Math.abs(MusicListenerService.instance.getLyric().offset)));
                    writeOffset();
                }
            }
        });
    }

    private String format(String input){
        String text1 = input.substring(1, input.length() - 1);
        String text2 = text1.replaceAll("\\d+: ", "");
        return text2.replaceAll(", ", "\n");
    }
    private static String mergeStrings(String s1, String s2) {
        s1 = s1.substring(1, s1.length() - 1);
        s2 = s2.substring(1, s2.length() - 1);
        s1 = s1.replaceAll(", ", "\n");
        s2 = s2.replaceAll(", ", "\n");
        Map<String, String> map1 = parseString(s1);
        Map<String, String> map2 = parseString(s2);

        StringBuilder result = new StringBuilder();
        for (String key : map1.keySet()) {
            if (map2.containsKey(key)) {
                result.append("\n");
            }
            result.append(key).append(": ").append(map1.get(key)).append("\n");
            if (map2.containsKey(key)) {
                result.append(key).append(": ").append(map2.get(key)).append("\n");
            }
        }
        return result.toString().replaceAll("\\d+: ", "");
    }
    private static Map<String, String> parseString(String str) {
        Map<String, String> map = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("(\\d+): (.+)");
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }
    private void writeOffset(){
        if (MusicListenerService.instance.getLyric().offset == 0) {
            if (translationStatusReferences.getBoolean(MusicListenerService.instance.musicInfo, false))
                offsetPreferences.edit().remove(MusicListenerService.instance.musicInfo + " ,tran").apply();
            else
                offsetPreferences.edit().remove(MusicListenerService.instance.musicInfo).apply();
        } else {
            if (translationStatusReferences.getBoolean(MusicListenerService.instance.musicInfo, false))
                offsetPreferences.edit().putInt(MusicListenerService.instance.musicInfo + " ,tran",
                        MusicListenerService.instance.getLyric().offset).apply();
            else
                offsetPreferences.edit().putInt(MusicListenerService.instance.musicInfo,
                        MusicListenerService.instance.getLyric().offset).apply();
        }
    }
}
