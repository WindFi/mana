package me.sunzheng.mana.home.feedback;

import android.util.Log;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;

/**
 * Created by Sun on 2018/2/23.
 */

public class FeedbackPresenterImpl implements HomeContract.Feedback.Presenter {
    final static String TAG = FeedbackPresenterImpl.class.getSimpleName();
    final FeedbackRequestWrapper request;
    HomeApiService.Feedback apiService;
    HomeContract.Feedback.View mView;
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    public FeedbackPresenterImpl(String episodeId, String videoFileId, HomeContract.Feedback.View view, HomeApiService.Feedback apiService) {
        request = new FeedbackRequestWrapper();
        request.setEpisode_id(episodeId);
        request.setVideo_file_id(videoFileId);
        this.mView = view;
        this.apiService = apiService;
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unsubscribe() {
        if (compositeDisposable != null)
            compositeDisposable.clear();
    }

    @Override
    public void setMessage(String message) {
        request.setMessage(message);
    }

    @Override
    public void submit() {
        mView.showProgressIntractor(true);
        Disposable disposable = apiService.send(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FeedbackResponseWrapper>() {
                    @Override
                    public void accept(FeedbackResponseWrapper feedbackResponseWrapper) throws Exception {
                        mView.showToast(feedbackResponseWrapper.getMessage());
                        if (feedbackResponseWrapper.getStatus() == 0) {
                            mView.finishSelf();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        mView.showProgressIntractor(false);
                        Log.i(TAG, throwable.getLocalizedMessage());
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        mView.showProgressIntractor(false);
                    }
                });
        compositeDisposable.add(disposable);
    }
}
