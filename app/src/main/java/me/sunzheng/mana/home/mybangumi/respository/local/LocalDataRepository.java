package me.sunzheng.mana.home.mybangumi.respository.local;

import android.content.Context;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import me.sunzheng.mana.home.mybangumi.respository.AppDbHelper;
import me.sunzheng.mana.home.mybangumi.respository.DataRespository;
import me.sunzheng.mana.home.onair.wrapper.BangumiModel;

/**
 * Created by Sun on 2017/11/11.
 */

public class LocalDataRepository implements DataRespository {
    Dao<BangumiModel, String> dao;
    Context context;

    public LocalDataRepository(Context context) {
        this.context = context;
        try {
            dao = OpenHelperManager.getHelper(context, AppDbHelper.class).getDao(BangumiModel.class);
            OpenHelperManager.releaseHelper();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Observable<List<BangumiModel>> query(int status) {
        return getModels(status).flatMapObservable(new Function<List<BangumiModel>, ObservableSource<? extends List<BangumiModel>>>() {
            @Override
            public ObservableSource<? extends List<BangumiModel>> apply(List<BangumiModel> bangumiModels) throws Exception {
                return Observable.just(bangumiModels);
            }
        });
    }

    @Override
    public Completable delete(final BangumiModel object) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                try {
                    if (object == null) {
                        TableUtils.clearTable(OpenHelperManager.getHelper(context, AppDbHelper.class).getConnectionSource(), BangumiModel.class);
                        OpenHelperManager.releaseHelper();
                    } else {
                        dao.delete(object);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public Completable insert(final BangumiModel object) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                try {
                    dao.createIfNotExists(object);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public Completable update(final BangumiModel object) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                try {
                    dao.create(object);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Maybe<List<BangumiModel>> getModels(int status) {
        Maybe maybe = Maybe.empty();
        try {
            List<BangumiModel> list = null;
            if (status == 0)
                list = dao.queryForAll();
            else {
                BangumiModel bangumiModel = new BangumiModel();
                bangumiModel.setFavoriteStatus(status);
                list = dao.queryForMatchingArgs(bangumiModel);
            }
            if (list == null || list.isEmpty())
                maybe = Maybe.empty();
            else
                maybe = Maybe.just(list);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return maybe;
    }
}