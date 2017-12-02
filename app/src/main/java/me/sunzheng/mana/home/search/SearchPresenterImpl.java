package me.sunzheng.mana.home.search;

import android.util.Log;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.core.BangumiModel;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;

/**
 * Created by Sun on 2017/6/15.
 */

public class SearchPresenterImpl implements HomeContract.Search.Presenter {
    final String TAG = getClass().getSimpleName();
    HomeContract.Search.View mView;
    CompositeDisposable compositeDisposable;
    HomeApiService.Bangumi service;
    QueryData data;
    List<BangumiModel> list;

    public SearchPresenterImpl(HomeContract.Search.View view, HomeApiService.Bangumi service) {
        this.mView = view;
        this.service = service;
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {
        compositeDisposable.dispose();
    }

    @Override
    public void query(String key) {
        mView.showProgressIntractor(true);
        mView.loadMoreable(true);
        data = new QueryData(INT_DEFAULT_PAGE, INT_DEFAULT_PAGESIZE, "air_date", "desc", key);
        Disposable disposable = query(data)
                .subscribe(new Consumer<SearchResultWrapper>() {
                    @Override
                    public void accept(SearchResultWrapper searchResultWrapper) throws Exception {
                        list = searchResultWrapper.getData();
                        if (list == null || list.isEmpty()) {
                            mView.empty();
                            mView.loadMoreable(false);
                        } else {
                            mView.setAdapter(new SearchResultAdapter(list));
                            mView.loadMoreable(list.size() < searchResultWrapper.getTotal());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.getLocalizedMessage());
                        mView.showProgressIntractor(false);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        mView.showProgressIntractor(false);
                    }
                });
        compositeDisposable.add(disposable);

    }

    @Override
    public void loadMore() {
        if (data == null)
            return;
        data.page++;
        mView.showLoadMoreProgressIntractor(true);
        Disposable disposable = query(data)
                .subscribe(new Consumer<SearchResultWrapper>() {
                    @Override
                    public void accept(SearchResultWrapper searchResultWrapper) throws Exception {
                        // TODO: 2017/7/24  loadmore
                        if (searchResultWrapper != null && searchResultWrapper.getTotal() > 0L) {
                            list.addAll(searchResultWrapper.getData());
                            mView.notifyDataSetChanged();
                            mView.loadMoreable(list.size() < searchResultWrapper.getTotal());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.getLocalizedMessage());
                        mView.showLoadMoreProgressIntractor(false);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        mView.showLoadMoreProgressIntractor(false);
                    }
                });
        compositeDisposable.add(disposable);
    }

    private Observable<SearchResultWrapper> query(QueryData queryData) {
        return service.listAll(queryData.page, queryData.count, queryData.field, queryData.sort, queryData.key)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
    }

    private static class QueryData {
        public int count, page;
        public String key, field, sort;

        public QueryData(int page, int count, String field, String sort, String key) {
            this.count = count;
            this.page = page;
            this.key = key;
            this.field = field;
            this.sort = sort;
        }
    }
}
