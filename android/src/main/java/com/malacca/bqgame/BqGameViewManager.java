package com.malacca.bqgame;

import androidx.annotation.NonNull;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

public class BqGameViewManager extends SimpleViewManager<BqGameView> {

    @Override
    public @NonNull String getName() {
        return "RNBqGameView";
    }

    @Override
    protected @NonNull BqGameView createViewInstance(@NonNull ThemedReactContext context) {
        return new BqGameView(context);
    }
}
