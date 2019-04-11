package com.my.githubdemo;

import android.util.Log;

import com.my.dataflow.Async;
import com.my.dataflow.BaseMvRxStatePresenter;
import com.my.dataflow.Loading;
import com.my.githubdemo.github.GithubApi;
import com.my.githubdemo.github.GithubApiFacade;
import com.my.githubdemo.github.GithubSearchResults;

import org.jetbrains.annotations.NotNull;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class GithubMainPresenter extends BaseMvRxStatePresenter<GithubMainState> {
    GithubApiFacade github;

    public GithubMainPresenter(@NotNull GithubMainState initialState) {
        super(initialState);
        String baseUrl = "https://api.github.com";
        Retrofit retrofit = new Retrofit.Builder()
                .client(new OkHttpClient.Builder().build())
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(baseUrl)
                .build();

        GithubApi api = retrofit.create(GithubApi.class);
        github = new GithubApiFacade(api);
    }

    public void fetchData() {
        withState(new Function1<GithubMainState, Unit>() {
            @Override
            public Unit invoke(GithubMainState githubMainState) {
                if (githubMainState.getRequest() instanceof Loading) return null;

                Log.d("hnl", "withState: " + githubMainState.toString());

                execute(github.loadNextPage(githubMainState.getPage()), new Function2<GithubMainState, Async<? extends GithubSearchResults>, GithubMainState>() {
                    @Override
                    public GithubMainState invoke(GithubMainState githubMainState, Async<? extends GithubSearchResults> async) {
                        Log.d("hnl", "execute: " + githubMainState.toString());

                        return githubMainState;
                    }
                });

                return Unit.INSTANCE;
            }
        });
    }
}
