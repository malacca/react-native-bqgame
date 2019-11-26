package com.malacca.bqgame;

import android.util.Log;
import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

import com.cmcm.cmgame.CmGameSdk;
import com.cmcm.cmgame.IAppCallback;
import com.cmcm.cmgame.IGameAdCallback;
import com.cmcm.cmgame.IGameStateCallback;
import com.cmcm.cmgame.IGameAccountCallback;
import com.cmcm.cmgame.IGameExitInfoCallback;
import com.cmcm.cmgame.IGamePlayTimeCallback;
import com.cmcm.cmgame.gamedata.CmGameAppInfo;

public class BqGameModule extends ReactContextBaseJavaModule implements LifecycleEventListener,
        IAppCallback,
        IGamePlayTimeCallback,
        IGameAdCallback,
        IGameAccountCallback,
        IGameExitInfoCallback,
        IGameStateCallback
{
    private static final String REACT_CLASS = "BqGameModule";
    private boolean sdkInit;

    /**
     * 模块类 开始
     */
    public BqGameModule(ReactApplicationContext context) {
        super(context);
    }

    @Override
    public @NonNull
    String getName() {
        return REACT_CLASS;
    }

    @Override
    public void onHostResume() {
    }

    @Override
    public void onHostPause() {
    }

    @Override
    public void onHostDestroy() {
        removeListener();
    }

    private boolean isConfigTrue(ReadableMap config, String key) {
        return config.hasKey(key) && config.getBoolean(key);
    }

    private String getConfigStr(ReadableMap config, String key) {
        return config.hasKey(key) ? config.getString(key) : null;
    }

    // 初始化 sdk
    @ReactMethod
    public void initSdk(ReadableMap config) {
        if (sdkInit) {
            return;
        }
        sdkInit = true;
        removeListener();
        initListener(config);
        CmGameSdk.initCmGameSdk(
                getCurrentActivity().getApplication(),
                getGameAppInfo(config),
                new ImageLoader(),
                BuildConfig.DEBUG
        );
    }

    public boolean isInit() {
        return sdkInit;
    }

    // 静音
    @ReactMethod
    public void setMute(boolean mute) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        if (app != null) {
            app.setMute(mute);
        }
    }

    // 游戏时屏幕常亮
    @ReactMethod
    public void setScreenOn(boolean screenOn) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        if (app != null) {
            app.setScreenOn(screenOn);
        }
    }

    // 退出二次确认
    @ReactMethod
    public void setQuitConfirm(boolean confirm) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        if (app != null) {
            app.setQuitGameConfirmFlag(confirm);
        }
    }

    // 显示退出推荐
    @ReactMethod
    public void setQuitRecommend(boolean recommend) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        if (app != null) {
            app.setQuitGameConfirmRecommand(recommend);
        }
    }

    // 先移除所有监听
    private void removeListener() {
        CmGameSdk.removeGameClickCallback();
        CmGameSdk.removeGamePlayTimeCallback();
        CmGameSdk.removeGameAdCallback();
        CmGameSdk.removeGameAccountCallback();
        CmGameSdk.removeGameStateCallback();
        CmGameSdk.removeGameExitInfoCallback();
    }

    // 设置监听
    private void initListener(ReadableMap config) {
        // 账号信息变化时触发回调，若需要支持APP卸载后游戏信息不丢失，需要注册该回调
        CmGameSdk.setGameAccountCallback(this);

        // 默认游戏中心页面，点击游戏试，触发回调
        CmGameSdk.setGameClickCallback(this);

        // 返回游戏数据(json格式)，如：每玩一关，返回关卡数，分数，如钓钓乐，返回水深，绳子长度，不同游戏不一样
        // 该功能使用场景：媒体利用游戏数据，和app的功能结合做运营活动，比如：某每天前50名，得到某些奖励
        CmGameSdk.setGameExitInfoCallback(this);

        // 点击游戏右上角或物理返回键，退出游戏时触发回调，并返回游戏时长
        CmGameSdk.setGamePlayTimeCallback(this);

        // 游戏界面的状态信息回调，1 onPause; 2 onResume
        CmGameSdk.setGameStateCallback(this);

        // 所有广告类型的展示和点击事件回调，仅供参考，数据以广告后台为准
        CmGameSdk.setGameAdCallback(this);
    }

    // 属性设置
    private CmGameAppInfo getGameAppInfo(ReadableMap config) {
        CmGameAppInfo cmGameAppInfo = new CmGameAppInfo();
        cmGameAppInfo.setAppId(getConfigStr(config, "appId"));
        cmGameAppInfo.setAppHost(getConfigStr(config, "appHost"));
        cmGameAppInfo.setMute(isConfigTrue(config, "mute"));
        cmGameAppInfo.setScreenOn(isConfigTrue(config, "screenOn"));
        cmGameAppInfo.setQuitGameConfirmFlag(
                !config.hasKey("quitConfirm") || config.getBoolean("quitConfirm")
        );
        cmGameAppInfo.setQuitGameConfirmRecommand(
                !config.hasKey("quitRecommend") || config.getBoolean("quitRecommend")
        );
        cmGameAppInfo.setTtInfo(getGameTTInfo(config));
        return cmGameAppInfo;
    }

    // 广告设置
    private CmGameAppInfo.TTInfo getGameTTInfo(ReadableMap config) {
        CmGameAppInfo.TTInfo ttInfo = new CmGameAppInfo.TTInfo();
        ttInfo.setRewardVideoId("936101115");   // 激励视频

//        ttInfo.setGameListFeedId("901121737"); // 游戏列表，信息流广告，自渲染

//        ttInfo.setFullVideoId("901121375");     // 全屏视频，插屏场景下展示
//        ttInfo.setExpressInteractionId("901121133"); // 插屏广告，模板渲染，插屏场景下展示
//        ttInfo.setExpressBannerId("901121159"); // Banner广告，模板渲染，尺寸：600*150
//        ttInfo.setGameEndFeedAdId("901121737"); // 游戏推荐弹框底部广告
//
//        // 游戏列表展示时显示，插屏广告，模板渲染1：1
//        // 游戏以tab形式的入口不要使用
//        ttInfo.setGamelistExpressInteractionId("901121536");
//
//        // 游戏加载时展示，下面广告2选1
//        // 插屏广告-原生-自渲染-大图
//        // 在2019-7-17后，穿山甲只针对部分媒体开放申请，如后台无法申请到这个广告位，则无需调用代码
//        ttInfo.setLoadingNativeId("901121435");
//        // 此广告申请，所有媒体都可申请，游戏加载时展示，插屏广告1:1，模板渲染
//        ttInfo.setGameLoad_EXADId("901121536");
        return ttInfo;
    }

    /**
     * 游戏账号信息回调，需要接入方保存，下次进入或卸载重装后设置给SDK使用，可以支持APP卸载后，游戏信息不丢失
     * @param loginInfo 用户账号信息
     */
    @Override
    public void onGameAccount(String loginInfo) {
        Log.d("cmgamesdk_Main2Activity", "onGameAccount loginInfo: " + loginInfo);
    }

    // 游戏点击回调
    @Override
    public void gameClickCallback(String gameName, String gameID) {
        Log.d("cmgamesdk_Main2Activity", gameID + "----" + gameName );
    }

    // 游戏暂停开始回调
    @Override
    public void gameStateCallback(int nState) {
        Log.d("cmgamesdk_Main2Activity", "gameStateCallback: " + nState);
    }

    // 游戏时长回调(秒)
    @Override
    public void gamePlayTimeCallback(String gameId, int playTimeInSeconds) {
        Log.d("cmgamesdk_Main2Activity", "play game ：" + gameId + "playTimeInSeconds : " + playTimeInSeconds);
    }

    /**
     * 广告曝光/点击回调
     * @param gameId 游戏Id
     * @param adType 广告类型：1：激励视频广告；2：Banner广告；3：原生Banner广告；4：全屏视频广告；
     *               5：原生插屏广告；6：开屏大卡广告；7：模板Banner广告；8：模板插屏广告
     * @param adAction 广告操作：1：曝光；2：点击；3：关闭；4：跳过
     */
    @Override
    public void onGameAdAction(String gameId, int adType, int adAction) {
        Log.d("cmgamesdk_Main2Activity", "onGameAdAction gameId: " + gameId + " adType: " + adType + " adAction: " + adAction);
    }

    /**
     * 返回游戏数据(json格式)，如：每玩一关，返回关卡数，分数，如钓钓乐，返回水深，绳子长度，不同游戏不一样
     * 该功能使用场景：媒体利用游戏数据，和app的功能结合做运营活动，比如：某每天前50名，得到某些奖励
     */
    @Override
    public  void gameExitInfoCallback(String gameExitInfo) {
        Log.d("cmgamesdk_Main2Activity", "gameExitInfoCallback: " + gameExitInfo);
    }

}
