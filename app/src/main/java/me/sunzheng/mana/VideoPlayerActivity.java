package me.sunzheng.mana;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

// TODO: 2017/6/1  VideoPlayer
public class VideoPlayerActivity extends AppCompatActivity {
    public final static String ARGS_URI_STR="uri";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
    }
}
