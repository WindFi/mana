package me.sunzheng.mana.home.episode.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import me.sunzheng.mana.VideoPlayerActivity;
import me.sunzheng.mana.home.bangumi.wrapper.Episode;
import me.sunzheng.mana.home.episode.Record;

public class PlayService extends Service {
    public final static String ARGS_ITEMS_PARCEL = "items";
    public final static String ARGS_POSITION_INT = "position";
    public final static String ACTION_FINISH_STR = "finish";
    public final static String ACTION_FINISH_PARCEL = "process";
    List<PlayItem> playList;
    RemotePlayer playerProxy = new RemotePlayerStub();
    private int current;

    // TODO: 2017/7/25  implement playlist
    public PlayService() {

    }

    public static Intent newInstance(Context context, List<Episode> list, int position) {
        Intent intent = new Intent(context, PlayService.class);
        Bundle extras = new Bundle();
        extras.putParcelableArrayList(ARGS_ITEMS_PARCEL, parsePlayItemArray(list));
        extras.putInt(ARGS_POSITION_INT, position);
        intent.putExtras(extras);
        return intent;
    }

    public static PlayItem parsePlayItem(Episode episode) {
        PlayItem item = new PlayItem();
        item.id = episode.getId();
        item.episodeNo = episode.getEpisodeNo();
        item.name = episode.getName();
        item.nameCn = episode.getNameCn();
        item.thumbnail = episode.getThumbnail();
        return item;
    }

    public static ArrayList<PlayItem> parsePlayItemArray(List<Episode> episodes) {
        ArrayList<PlayItem> arrayList = new ArrayList<>(episodes.size());
        for (Episode item : episodes) {
            arrayList.add(parsePlayItem(item));
        }
        return arrayList;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return playerProxy;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (playList != null && !playList.isEmpty()) {
            playList.clear();
            playList = null;
        }
        playList = intent.getParcelableArrayListExtra(ARGS_ITEMS_PARCEL);
        current = intent.getIntExtra(ARGS_POSITION_INT, 0);
        Intent playerIntent = new Intent(PlayService.this, VideoPlayerActivity.class);
        startActivity(playerIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    private int getPosition() {
        return current;
    }

    private boolean moveToNext() {
        if (current + 1 < playList.size() - 1) {
            current++;
            return true;
        } else {
            current = playList.size() - 1;
            return false;
        }
    }

    private boolean hasNext() {
        return playList != null && current < playList.size() - 1;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public final static class PlayItem implements Parcelable {
        public static final Creator<PlayItem> CREATOR = new Creator<PlayItem>() {
            @Override
            public PlayItem createFromParcel(Parcel in) {
                return new PlayItem(in);
            }

            @Override
            public PlayItem[] newArray(int size) {
                return new PlayItem[size];
            }
        };
        public String id;
        public String thumbnail;
        public int episodeNo;
        public String name;
        public String nameCn;

        public PlayItem() {
        }

        protected PlayItem(Parcel in) {
            id = in.readString();
            thumbnail = in.readString();
            episodeNo = in.readInt();
            name = in.readString();
            nameCn = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(id);
            dest.writeString(thumbnail);
            dest.writeInt(episodeNo);
            dest.writeString(name);
            dest.writeString(nameCn);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    class RemotePlayerStub extends Binder implements RemotePlayer {

        @Override
        public void logWatchProcess(Record record) {

        }

        @Override
        public List<PlayItem> queryAllPlayList() {
            return playList;
        }

        @Override
        public int getPosition() {
            return PlayService.this.getPosition();
        }

        @Override
        public void onDisconnect() {

        }

        @Override
        public boolean hasNext() {
            return playList != null || current < playList.size();
        }

        @Override
        public PlayItem moveToNext() {
            return hasNext() ? playList.get(++current) : null;
        }
    }
}
