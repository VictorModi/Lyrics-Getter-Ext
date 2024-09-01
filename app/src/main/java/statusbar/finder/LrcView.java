package statusbar.finder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;


public class LrcView extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lrcview);

        TextView lrc =findViewById(R.id.lrc);
        String lrc1 = MusicListenerService.instance.getLyric().sentenceList.toString();
        String lrc2 = lrc1.substring(1, lrc1.length() -1);
        String lrc3 = lrc2.replaceAll("\\d+: ", "");
        String lrc4 = lrc3.replaceAll(", ","\n");
        lrc.setText(lrc4);

        TextView offset =findViewById(R.id.offset);
        Button plus =findViewById(R.id.plus);
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MusicListenerService.instance.setLyricOffset(MusicListenerService.instance.getLyric().offset - 100);
                offset.setText("offset: " + String.valueOf((MusicListenerService.instance.getLyric().offset > 0) ? -MusicListenerService.instance.getLyric().offset : Math.abs(MusicListenerService.instance.getLyric().offset)));
            }
        });

        Button minus =findViewById(R.id.minus);
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MusicListenerService.instance.setLyricOffset(MusicListenerService.instance.getLyric().offset + 100);
                offset.setText("offset: " + String.valueOf((MusicListenerService.instance.getLyric().offset > 0) ? -MusicListenerService.instance.getLyric().offset : Math.abs(MusicListenerService.instance.getLyric().offset)));
            }
        });
    }
}
