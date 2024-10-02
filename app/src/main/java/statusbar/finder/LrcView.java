package statusbar.finder;

import static statusbar.finder.misc.Constants.PREFERENCE_KEY_REQUIRE_TRANSLATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;


public class LrcView extends FragmentActivity {
    private SharedPreferences offsetpreferences;
    private SharedPreferences translationstatusreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lrcview);

        offsetpreferences = getSharedPreferences("offset", MODE_PRIVATE);
        translationstatusreferences = getSharedPreferences("translationstatus", MODE_PRIVATE);

        TextView lrc =findViewById(R.id.lrc);
        TextView offset =findViewById(R.id.offset);
        TextView tran =findViewById(R.id.tran);
        if (MusicListenerService.instance.getLyric() == null)
            lrc.setText("none");
        else {
            lrc.setText(format(MusicListenerService.instance.getLyric().sentenceList.toString()));
            offset.setText("offset: " + ((MusicListenerService.instance.getLyric().offset > 0) ? -MusicListenerService.instance.getLyric().offset : Math.abs(MusicListenerService.instance.getLyric().offset)));
            if (!PreferenceManager.getDefaultSharedPreferences(MusicListenerService.instance).getBoolean(PREFERENCE_KEY_REQUIRE_TRANSLATE, false))
                if (MusicListenerService.instance.getLyric().translatedSentenceList != null)
                    if (!format(MusicListenerService.instance.getLyric().translatedSentenceList.toString()).isEmpty())
                        tran.setVisibility(View.VISIBLE);
        }

        if (translationstatusreferences.getBoolean(MusicListenerService.instance.musicinfo, false) && !PreferenceManager.getDefaultSharedPreferences(MusicListenerService.instance).getBoolean(PREFERENCE_KEY_REQUIRE_TRANSLATE, false)) {
            tran.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.teal_700));
            lrc.setText(format(MusicListenerService.instance.getLyric().translatedSentenceList.toString()));
        }
        tran.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tran.getCurrentTextColor() == ContextCompat.getColor(getApplicationContext(), R.color.teal_700)) {
                    tran.setTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.tab_indicator_text));
                    translationstatusreferences.edit().remove(MusicListenerService.instance.musicinfo).apply();
                    lrc.setText(format(MusicListenerService.instance.getLyric().sentenceList.toString()));
                } else {
                    tran.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.teal_700));
                    translationstatusreferences.edit().putBoolean(MusicListenerService.instance.musicinfo, true).apply();
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
                    writeoffset();
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
                    writeoffset();
                }
            }
        });
    }

    private String format(String input){
        String text1 = input.substring(1, input.length() - 1);
        String text2 = text1.replaceAll("\\d+: ", "");
        return text2.replaceAll(", ", "\n");
    }
    private void writeoffset(){
        if (MusicListenerService.instance.getLyric().offset == 0) {
            if (translationstatusreferences.getBoolean(MusicListenerService.instance.musicinfo, false))
                offsetpreferences.edit().remove(MusicListenerService.instance.musicinfo + " ,tran").apply();
            else
                offsetpreferences.edit().remove(MusicListenerService.instance.musicinfo).apply();
        } else {
            if (translationstatusreferences.getBoolean(MusicListenerService.instance.musicinfo, false))
                offsetpreferences.edit().putInt(MusicListenerService.instance.musicinfo + " ,tran",
                        MusicListenerService.instance.getLyric().offset).apply();
            else
                offsetpreferences.edit().putInt(MusicListenerService.instance.musicinfo,
                        MusicListenerService.instance.getLyric().offset).apply();
        }
    }
}
