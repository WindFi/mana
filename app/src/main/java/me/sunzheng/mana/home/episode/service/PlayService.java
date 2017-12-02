package me.sunzheng.mana.home.episode.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.VideoPlayerActivity;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.bangumi.Response;
import me.sunzheng.mana.home.bangumi.wrapper.request.SynchronizeEpisodeHistoryWrapper;
import me.sunzheng.mana.home.episode.Record;
import me.sunzheng.mana.home.onair.wrapper.Episode;
import me.sunzheng.mana.home.onair.wrapper.WatchProgress;
import me.sunzheng.mana.utils.App;

public class PlayService extends Service {
    public final static String ARGS_ITEMS_PARCEL = "items";
    public final static String ARGS_POSITION_INT = "position";
    public final static String ACTION_FINISH_STR = "finish";
    public final static String ACTION_FINISH_PARCEL = "process";
    List<PlayItem> playList;
    RemotePlayer playerProxy = new RemotePlayerStub();
    HomeApiService.Bangumi apiService;
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
        item.setId(episode.getId());
        item.setBangumiId(episode.getBangumiId());
        item.episodeNo = episode.getEpisodeNo();
        item.name = episode.getName();
        item.nameCn = episode.getNameCn();
        item.thumbnail = episode.getThumbnail();
        WatchProgress watchProgress = episode.getWatchProgress();
        if (watchProgress != null) {
            item.lastWatchTime = (long) watchProgress.getLastWatchTime();
            item.lastWatchPosition = (long) watchProgress.getLastWatchPosition();
        }
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
        apiService = ((App) getApplication()).getRetrofit().create(HomeApiService.Bangumi.class);
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
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                playList = extras.getParcelableArrayList(ARGS_ITEMS_PARCEL);
                current = extras.getInt(ARGS_POSITION_INT, 0);
                Intent playerIntent = new Intent(PlayService.this, VideoPlayerActivity.class);
                startActivity(playerIntent);
            } else {
                throw new IllegalArgumentException("Argument is null");
            }
        }
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
        private UUID id;
        private UUID bangumiId;
        private String thumbnail;
        private int episodeNo;
        private String name;
        private String nameCn;
        private long lastWatchTime;
        private long lastWatchPosition;

        public PlayItem() {
        }

        protected PlayItem(Parcel in) {
            id = UUID.fromString(in.readString());
            bangumiId = UUID.fromString(in.readString());
            ;
            thumbnail = in.readString();
            episodeNo = in.readInt();
            name = in.readString();
            nameCn = in.readString();
            lastWatchTime = in.readLong();
            lastWatchPosition = in.readLong();
        }

        public String getId() {
            return id.toString();
        }

        public void setId(String id) {
            this.id = UUID.fromString(id);
        }

        public String getBangumiId() {
            return bangumiId.toString();
        }

        public void setBangumiId(String bangumiId) {
            this.bangumiId = UUID.fromString(bangumiId);
        }

        public String getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
        }

        public int getEpisodeNo() {
            return episodeNo;
        }

        public void setEpisodeNo(int episodeNo) {
            this.episodeNo = episodeNo;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNameCn() {
            return nameCn;
        }

        public void setNameCn(String nameCn) {
            this.nameCn = nameCn;
        }

        public long getLastWatchTime() {
            return lastWatchTime;
        }

        public void setLastWatchTime(long lastWatchTime) {
            this.lastWatchTime = lastWatchTime;
        }

        public long getLastWatchPosition() {
            return lastWatchPosition;
        }

        public void setLastWatchPosition(long lastWatchPosition) {
            this.lastWatchPosition = lastWatchPosition;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(id.toString());
            dest.writeString(bangumiId.toString());
            dest.writeString(thumbnail);
            dest.writeInt(episodeNo);
            dest.writeString(name);
            dest.writeString(nameCn);
            dest.writeLong(lastWatchTime);
            dest.writeLong(lastWatchPosition);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    class RemotePlayerStub extends Binder implements RemotePlayer {

        @Override
        public void logWatchProcess(float currentPosition, float duration) {
            SynchronizeEpisodeHistoryWrapper request = new SynchronizeEpisodeHistoryWrapper();
            Record record = new Record();
            record.setBangumiId(getCurrentPlayItem().getBangumiId());
            record.setEpisodeId(getCurrentPlayItem().getId());
            record.setLastWatchPosition(currentPosition);
            record.setLastWatchTime(System.currentTimeMillis());
            record.setPercentage(currentPosition / duration);
            record.setIsFinished(currentPosition >= duration);

            ArrayList<Record> list = new ArrayList<>(1);
            list.add(record);
            request.setItem(list);
            Log.i(getClass().getSimpleName(), new Gson().toJson(request) + "duration:" + duration);
            apiService.synchronizeEpisodeHistory(request)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Response>() {
                        @Override
                        public void accept(Response response) throws Exception {

                        }
                    });
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

        @Override
        public PlayItem getCurrentPlayItem() {
            return playList.get(PlayService.this.getPosition());
        }
    }
}
