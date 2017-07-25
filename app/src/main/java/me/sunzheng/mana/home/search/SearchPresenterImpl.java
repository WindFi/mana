package me.sunzheng.mana.home.search;

import android.util.Log;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.search.wrapper.SearchResultAdapter;

/**
 * Created by Sun on 2017/6/15.
 */

public class SearchPresenterImpl implements HomeContract.Search.Presenter {
    final String TAG = getClass().getSimpleName();
    HomeContract.Search.View mView;
    CompositeDisposable compositeDisposable;
    HomeApiService.Bangumi service;
    QueryData data;

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
        data = new QueryData(1, 30, "air_date", "desc", key);
        Disposable disposable = query(data)
                .subscribe(new Consumer<SearchResultWrapper>() {
                    @Override
                    public void accept(SearchResultWrapper searchResultWrapper) throws Exception {
                        mView.setAdapter(new SearchResultAdapter(searchResultWrapper.getData()));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.getLocalizedMessage());
                    }
                });
        compositeDisposable.add(disposable);

    }

    @Override
    public void loadMore() {
        if (data == null)
            return;
        data.page++;
        Disposable disposable = query(data)
                .subscribe(new Consumer<SearchResultWrapper>() {
                    @Override
                    public void accept(SearchResultWrapper searchResultWrapper) throws Exception {
                        // TODO: 2017/7/24  loadmore
                        mView.setAdapter(new SearchResultAdapter(searchResultWrapper.getData()));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.getLocalizedMessage());
                    }
                });
        compositeDisposable.add(disposable);
    }

    private Observable<SearchResultWrapper> query(QueryData queryData) {
        return service.listAll(queryData.page, queryData.count, queryData.field, queryData.sort, queryData.key)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .doOnTerminate(new Action() {
                    @Override
                    public void run() throws Exception {
                        mView.showProgressIntractor(false);
                    }
                });
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
