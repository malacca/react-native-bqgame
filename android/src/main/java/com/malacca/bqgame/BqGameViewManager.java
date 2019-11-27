package com.malacca.bqgame;

import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import com.cmcm.cmgame.GameView;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

public class BqGameViewManager extends SimpleViewManager {

    private static class BqGameView extends NestedScrollView {
        private LinearLayout wrapper;

        public BqGameView(@NonNull ThemedReactContext context) {
            super(context);
            if (wrapper == null) {
                wrapper = new LinearLayout(context);
                wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                addView(wrapper);
            }
            wrapper.removeAllViews();
            try {
                GameView view = new GameView(context);
                wrapper.addView(view);
                view.inflate(context.getCurrentActivity());
            } catch (Throwable e) {
                // do nothing
            }
        }

        /**
         * 列表可能会插入广告, 需要这里动态刷新, 否则广告不显示
         * https://github.com/facebook/react-native/issues/17968
         * https://github.com/facebook/react-native/issues/11829
         * https://www.jianshu.com/p/a6c5042c5ce8
         */
        @Override
        public void requestLayout() {
            super.requestLayout();
            post(measureAndLayout);
        }

        private final Runnable measureAndLayout = new Runnable() {
            @Override
            public void run() {
                measure(
                        MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY)
                );
                layout(getLeft(), getTop(), getRight(), getBottom());
            }
        };
    }

    @Override
    public @NonNull String getName() {
        return "RNBqGameView";
    }

    @Override
    protected @NonNull BqGameView createViewInstance(@NonNull ThemedReactContext context) {
        return new BqGameView(context);
    }
}
