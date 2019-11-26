package com.malacca.bqgame;

import android.util.Log;
import androidx.annotation.NonNull;
import android.widget.LinearLayout;
import androidx.core.widget.NestedScrollView;

import com.cmcm.cmgame.GameView;
import com.facebook.react.uimanager.ThemedReactContext;

public class BqGameView extends NestedScrollView {
    public BqGameView(@NonNull ThemedReactContext context) {
        super(context);
        try {
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            addView(linearLayout);
            GameView view = new GameView(context);
            linearLayout.addView(view);
            view.inflate(context.getCurrentActivity());
        } catch (Throwable e) {
            Log.d("RN", "__" + e);
        }
    }
}
