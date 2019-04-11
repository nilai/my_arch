package com.my.githubdemo;

import android.os.Bundle;
import android.util.Log;

import com.my.dataflow.ActivityStateContext;
import com.my.dataflow.BaseMvRxActivity;
import com.my.dataflow.StateFactory;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class GithubMainActivity extends BaseMvRxActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity_main);

        GithubMainState state = StateFactory.INSTANCE.createInitialState(GithubMainState.class, new ActivityStateContext(this, null),
                new Function1<GithubMainState, GithubMainState>() {
                    @Override
                    public GithubMainState invoke(GithubMainState githubMainState) {
                        return githubMainState;
                    }
                });
        GithubMainPresenter presenter = new GithubMainPresenter(state);
        presenter.subscribe(this, new Function1<GithubMainState, Unit>() {
            @Override
            public Unit invoke(GithubMainState githubMainState) {
                Log.d("hnl", "subscribe: " + githubMainState.toString());

                return Unit.INSTANCE;
            }
        });
        presenter.fetchData();
    }
}
