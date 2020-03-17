package com.malacca.bqgame;

import android.view.View;
import android.content.Context;
import android.view.Choreographer;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import com.cmcm.cmgame.GameView;
import com.facebook.react.uimanager.Spacing;
import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

class BqGameViewManager extends SimpleViewManager<BqGameViewManager.BqGameView> {
    private LayoutShadowNode layoutShadowNode;

    @Override
    public @NonNull String getName() {
        return "RNBqGameView";
    }


    @Override
    protected @NonNull BqGameView createViewInstance(@NonNull ThemedReactContext context) {
        return new BqGameView(context);
    }

    @Override
    public LayoutShadowNode createShadowNodeInstance() {
        return layoutShadowNode = new LayoutShadowNode();
    }

    @Override
    protected void onAfterUpdateTransaction(@NonNull BqGameView view) {
        super.onAfterUpdateTransaction(view);
        view.setPadding(layoutShadowNode);
    }

    /**
     * game center view
     */
    static class BqGameView extends NestedScrollView {
        private LinearLayout wrapper;

        public BqGameView(Context context) {
            super(context);
        }

        public BqGameView(@NonNull ThemedReactContext context) {
            super(context);
            try {
                wrapper = new LinearLayout(context);
                wrapper.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
                wrapper.setOrientation(LinearLayout.VERTICAL);
                wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                GameView view = new GameView(context);
                wrapper.addView(view);
                addView(wrapper);
                view.inflate(context.getCurrentActivity());
                initMeasure();
            } catch (Throwable e) {
                // do nothing
            }
        }

        /**
         * 这里不理解是什么原因, 根据以下参考资料, initMeasure/requestLayout 二选一即可
         * 但实测一下, initMeasure 可以让头部的入口图标显示, requestLayout 可以让广告显示
         * initMeasure 无限循环也可以让广告显示, 但总感觉无限循环不合适, 暂且这么着吧
         * https://github.com/facebook/react-native/issues/17968
         * https://github.com/facebook/react-native/issues/11829
         * https://www.jianshu.com/p/a6c5042c5ce8
         */
        void initMeasure() {
            Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    manuallyChildren();
                    getViewTreeObserver().dispatchOnGlobalLayout();
                }
            });
        }

        void manuallyChildren() {
            for (int i = 0; i < wrapper.getChildCount(); i++) {
                View child = wrapper.getChildAt(i);
                child.measure(
                        MeasureSpec.makeMeasureSpec(wrapper.getMeasuredWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(wrapper.getMeasuredHeight(), MeasureSpec.EXACTLY)
                );
                child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
            }
        }

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

        // 这里直接应用 父级(NestedScrollView)的 Yoga padding 到 LinearLayout 上
        public void setPadding(LayoutShadowNode layoutShadowNode) {
            if (wrapper == null) {
                return;
            }
            wrapper.setPadding(
                    (int) layoutShadowNode.getPadding(Spacing.LEFT),
                    (int) layoutShadowNode.getPadding(Spacing.TOP),
                    (int) layoutShadowNode.getPadding(Spacing.RIGHT),
                    (int) layoutShadowNode.getPadding(Spacing.BOTTOM)
            );
        }
    }
}
