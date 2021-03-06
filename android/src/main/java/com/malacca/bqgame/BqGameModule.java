package com.malacca.bqgame;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;

import android.app.Activity;
import android.text.TextUtils;
import android.content.Context;
import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.cmcm.cmgame.CmGameSdk;
import com.cmcm.cmgame.IAppCallback;
import com.cmcm.cmgame.utils.X5Helper;
import com.cmcm.cmgame.IGameAdCallback;
import com.cmcm.cmgame.IGameStateCallback;
import com.cmcm.cmgame.IGameAccountCallback;
import com.cmcm.cmgame.IGameExitInfoCallback;
import com.cmcm.cmgame.IGamePlayTimeCallback;
import com.cmcm.cmgame.gamedata.bean.GameInfo;
import com.cmcm.cmgame.gamedata.CmGameAppInfo;
import com.cmcm.cmgame.IGameListReadyCallback;

class BqGameModule extends ReactContextBaseJavaModule implements LifecycleEventListener,
        IGameListReadyCallback,
        IAppCallback,
        IGamePlayTimeCallback,
        IGameAdCallback,
        IGameAccountCallback,
        IGameExitInfoCallback,
        IGameStateCallback
{
    private static final String REACT_CLASS = "BqGameModule";
    private static Boolean x5Init = null;
    private ReactApplicationContext rnContext;
    private DeviceEventManagerModule.RCTDeviceEventEmitter mJSModule;
    private List<Promise> getInfoCallbacks;

    /**
     * 模块类 开始
     */
    BqGameModule(ReactApplicationContext context) {
        super(context);
        rnContext = context;
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

    /**
     * 初始化 sdk, 通过传递 withX5 参数来决定如何集成 x5
     * 1. withX5="yes"  已在外部集成成功, 传递 yes 即可
     * 2. withX5="no"   外部集成失败 或 强制不使用 x5
     * 3. withX5="auto" 尝试自动集成
     */
    @ReactMethod
    public void initSdk(final ReadableMap config) {
        String x5 = config.hasKey("withX5") ? config.getString("withX5") : null;
        if ("yes".equals(x5)) {
            initX5End(true);
        } else if ("no".equals(x5)) {
            initX5End(false);
        } else {
            initX5Sdk();
        }
        removeListener();
        initListener(config);
        String account = getConfigStr(config, "account");
        if (!TextUtils.isEmpty(account)) {
            CmGameSdk.restoreCmGameAccount(account);
        }
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = getCurrentActivity();
                if (activity != null) {
                    CmGameSdk.initCmGameSdk(
                            activity.getApplication(),
                            getGameAppInfo(config),
                            new ImageLoader(),
                            isConfigTrue(config, "debug") || BuildConfig.DEBUG
                    );
                }
            }
        });
    }

    // 通过反射初始化 x5 内核, 这样在无 x5 sdk 依赖的情况下仍然可编译
    private void initX5Sdk() {
        if (x5Init != null) {
            callX5Promise();
            return;
        }
        try {
            Class<?> qbSdk = Class.forName("com.tencent.smtt.sdk.QbSdk");
            Class<?> PreInitCallback = Class.forName("com.tencent.smtt.sdk.QbSdk$PreInitCallback");
            Method method = qbSdk.getDeclaredMethod("initX5Environment", Context.class, PreInitCallback);
            Object listener = Proxy.newProxyInstance(
                    PreInitCallback.getClassLoader(),
                    new Class[] { PreInitCallback },
                    new PreInitCallback()
            );
            method.invoke(qbSdk, rnContext, listener);
        } catch (Throwable e) {
            initX5End(false);
        }
    }

    private class PreInitCallback implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("onViewInitFinished".equals(method.getName())) {
                initX5End((boolean) args[0]);
            }
            return proxy;
        }
    }

    // 通过反射重置 CMGame 的 mX5InitSuccess 属性值
    private void initX5End(boolean success) {
        try {
            Class x5fit = Class.forName("com.cmgame.x5fit.X5CmGameSdk");
            Field field = x5fit.getDeclaredField("mX5InitSuccess");
            field.setAccessible(true);
            field.set(null, success);
            X5Helper.mX5InitSuccess = success;
            x5Init = success;
        } catch (Throwable e) {
            x5Init = false;
        }
        callX5Promise();
    }

    // 属性设置
    private CmGameAppInfo getGameAppInfo(ReadableMap config) {
        CmGameAppInfo cmGameAppInfo = new CmGameAppInfo();
        cmGameAppInfo.setAppId(getConfigStr(config, "appId"));
        cmGameAppInfo.setAppHost(getConfigStr(config, "appHost"));
        cmGameAppInfo.setMute(isConfigTrue(config, "mute"));
        cmGameAppInfo.setScreenOn(isConfigTrue(config, "screenOn"));
        cmGameAppInfo.setRewarded(isConfigTrue(config, "rewarded"));
        // 退出时, 是否提示 、是否显示广告、提示词
        cmGameAppInfo.setQuitGameConfirmFlag(
                !config.hasKey("quitConfirm") || config.getBoolean("quitConfirm")
        );
        cmGameAppInfo.setQuitGameConfirmRecommand(
                !config.hasKey("quitRecommend") || config.getBoolean("quitRecommend")
        );
        String tip = config.hasKey("quitConfirmTip") ? config.getString("quitConfirmTip") : null;
        if (tip != null) {
            cmGameAppInfo.setQuitGameConfirmTip(tip);
        }
        setGameTTadInfo(cmGameAppInfo, config);
        return cmGameAppInfo;
    }

    // 广告设置
    private void setGameTTadInfo(CmGameAppInfo gameAppInfo, ReadableMap config) {
        ReadableMap ttad = config.hasKey("ttad") ? config.getMap("ttad") : null;
        if (ttad == null) {
            return;
        }
        // 广告信息
        // -------------------------------------------------------
        CmGameAppInfo.TTInfo ttInfo = new CmGameAppInfo.TTInfo();

        // 游戏列表展示时显示插屏广告, 游戏以tab形式的入口最好不要使用 (模板渲染1：1)
        ttInfo.setGamelistExpressInteractionId(getConfigStr(ttad, "listInteraction"));

        // 游戏列表，信息流广告，(自渲染) (模板渲染)
        ttInfo.setGameListFeedId(getConfigStr(ttad, "listFeed"));
        ttInfo.setGameListExpressFeedId(getConfigStr(ttad, "listExpress"));

        // 游戏加载时展示，下面广告2选1 (插屏广告-原生-自渲染-大图) (插屏广告1:1，模板渲染)
        // 在2019-7-17后，穿山甲只针对部分媒体开放申请，如后台无法申请到这个广告位，则无需调用代码
        ttInfo.setLoadingNativeId(getConfigStr(ttad, "loadingNative"));
        ttInfo.setGameLoad_EXADId(getConfigStr(ttad, "loadingInteraction"));

        // 部分游戏内Banner广告，(模板渲染，尺寸：600*150)
        ttInfo.setExpressBannerId(getConfigStr(ttad, "expressBanner"));

        // 游戏内 激励视频
        ttInfo.setRewardVideoId(getConfigStr(ttad, "rewardVideo"));

        // 全屏视频，插屏场景下展示
        ttInfo.setFullVideoId(getConfigStr(ttad, "fullVideo"));

        // 插屏广告, 插屏场景下展示 (模板渲染)
        ttInfo.setExpressInteractionId(getConfigStr(ttad, "expressInteraction"));

        // 退出游戏 推荐弹框底部广告 (自渲染) (模板渲染)
        ttInfo.setGameEndFeedAdId(getConfigStr(ttad, "endFeed"));
        ttInfo.setGameEndExpressFeedAdId(getConfigStr(ttad, "endExpress"));

        // 载入广告信息
        gameAppInfo.setTtInfo(ttInfo);

        // 广告配置
        // -------------------------------------------------------
        CmGameAppInfo.GameListAdInfo adInfo = new CmGameAppInfo.GameListAdInfo();

        // 热门推荐是否显示
        adInfo.setHotGameListAdShow(!ttad.hasKey("adHot") || ttad.getBoolean("adHot"));

        // 最新  是否显示
        adInfo.setNewGameListAdShow(!ttad.hasKey("adNew") || ttad.getBoolean("adNew"));

        // 更多好玩  是否显示 / 几行显示一个广告
        adInfo.setMoreGameListAdShow(!ttad.hasKey("adMore") || ttad.getBoolean("adMore"));
        int slice = ttad.hasKey("adMoreSlice") ? Math.max(1, ttad.getInt("adMoreSlice")) : 4;
        adInfo.setMoreGameListAdInternal(slice);
        gameAppInfo.setGameListAdInfo(adInfo);
    }

    // 先移除所有监听
    private void removeListener() {
        CmGameSdk.removeGameAccountCallback();
        CmGameSdk.removeGameListReadyCallback();
        CmGameSdk.removeGameClickCallback();
        CmGameSdk.removeGameExitInfoCallback();
        CmGameSdk.removeGamePlayTimeCallback();
        CmGameSdk.removeGameStateCallback();
        CmGameSdk.removeGameAdCallback();
    }

    // 设置监听
    private void initListener(ReadableMap config) {
        // 账号信息变化时触发回调，若需要支持APP卸载后游戏信息不丢失，需要注册该回调
        if (isConfigTrue(config, "onLogin")) {
            CmGameSdk.setGameAccountCallback(this);
        }
        // 默认游戏中心页面，点击游戏试，触发回调
        if (isConfigTrue(config, "onClick")) {
            CmGameSdk.setGameClickCallback(this);
        }
        // 游戏界面的状态信息回调，1 onPause; 2 onResume
        if (isConfigTrue(config, "onState")) {
            CmGameSdk.setGameStateCallback(this);
        }
        // 返回游戏数据(json格式)，如：每玩一关，返回关卡数，分数，如钓钓乐，返回水深，绳子长度，不同游戏不一样
        // 该功能使用场景：媒体利用游戏数据，和app的功能结合做运营活动，比如：某每天前50名，得到某些奖励
        if (isConfigTrue(config, "onPass")) {
            CmGameSdk.setGameExitInfoCallback(this);
        }
        // 点击游戏右上角或物理返回键，退出游戏时触发回调，并返回游戏时长
        if (isConfigTrue(config, "onClose")) {
            CmGameSdk.setGamePlayTimeCallback(this);
        }
        // 所有广告类型的展示和点击事件回调，仅供参考，数据以广告后台为准
        if (isConfigTrue(config, "onAd")) {
            CmGameSdk.setGameAdCallback(this);
        }
        if (isConfigTrue(config, "onListReady")) {
            CmGameSdk.setGameListReadyCallback(this);
        }
    }

    // 游戏列表加载完毕回调
    @Override
    public void onGameListReady() {
        WritableMap params = Arguments.createMap();
        params.putString("event", "onListReady");
        sendEvent(params);
    }

    /**
     * 游戏账号信息回调，需要接入方保存，下次进入或卸载重装后设置给SDK使用，可以支持APP卸载后，游戏信息不丢失
     * @param loginInfo 用户账号信息
     */
    @Override
    public void onGameAccount(String loginInfo) {
        WritableMap params = Arguments.createMap();
        params.putString("event", "onLogin");
        params.putString("account", loginInfo);
        sendEvent(params);
    }

    // 游戏点击回调
    @Override
    public void gameClickCallback(String gameName, String gameID) {
        WritableMap params = Arguments.createMap();
        params.putString("event", "onClick");
        params.putString("gameID", gameID);
        params.putString("gameName", gameName);
        sendEvent(params);
    }

    // 游戏暂停开始回调
    @Override
    public void gameStateCallback(int nState) {
        WritableMap params = Arguments.createMap();
        params.putString("event", "onState");
        params.putInt("state", nState);
        sendEvent(params);
    }

    /**
     * 返回游戏数据(json格式)，如：每玩一关，返回关卡数，分数，如钓钓乐，返回水深，绳子长度，不同游戏不一样
     * 该功能使用场景：媒体利用游戏数据，和app的功能结合做运营活动，比如：某每天前50名，得到某些奖励
     */
    @Override
    public  void gameExitInfoCallback(String gameExitInfo) {
        WritableMap params = Arguments.createMap();
        params.putString("event", "onPass");
        params.putString("info", gameExitInfo);
        sendEvent(params);
    }

    // 游戏时长回调(秒)
    @Override
    public void gamePlayTimeCallback(String gameId, int playTimeInSeconds) {
        WritableMap params = Arguments.createMap();
        params.putString("event", "onClose");
        params.putString("gameId", gameId);
        params.putInt("time", playTimeInSeconds);
        sendEvent(params);
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
        WritableMap params = Arguments.createMap();
        params.putString("event", "onAd");
        params.putString("gameId", gameId);
        params.putInt("adType", adType);
        params.putInt("adAction", adAction);
        sendEvent(params);
    }

    private void sendEvent(WritableMap params) {
        if (mJSModule == null) {
            mJSModule = rnContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        }
        mJSModule.emit("BqGameEvent", params);
    }

    // 预防在前调用了 isX5, 这里触发回调
    private void callX5Promise() {
        if (getInfoCallbacks == null) {
            return;
        }
        Iterator<Promise> i = getInfoCallbacks.iterator();
        while (i.hasNext()) {
            i.next().resolve(x5Init);
            i.remove();
        }
        getInfoCallbacks = null;
    }

    // x5 是否成功集成
    @ReactMethod
    public void isX5(Promise promise) {
        if (x5Init != null) {
            promise.resolve(x5Init);
            return;
        }
        if (getInfoCallbacks == null) {
            getInfoCallbacks = new ArrayList<>();
        }
        getInfoCallbacks.add(promise);
    }

    // 自行渲染列表时, 可使用该方法登录游客账号
    // 直接使用 GameCenter 列表则无需
    @ReactMethod
    public void initAccount() {
        CmGameSdk.initCmGameAccount();
    }

    // 设置登陆账号
    @ReactMethod
    public void setAccount(String account) {
        CmGameSdk.restoreCmGameAccount(account);
    }

    // 清除当前账号, 会生成新的账号
    @ReactMethod
    public void clearAccount() {
        CmGameSdk.clearCmGameAccount();
    }

    // 是否静音
    @ReactMethod
    public void isMute(Promise promise) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        promise.resolve(app == null ? null : app.isMute());
    }

    // 静音
    @ReactMethod
    public void setMute(boolean mute) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        if (app != null) {
            app.setMute(mute);
        }
    }

    // 是否屏幕常亮
    @ReactMethod
    public void isScreenOn(Promise promise) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        promise.resolve(app == null ? null : app.isScreenOn());
    }

    // 游戏时屏幕常亮
    @ReactMethod
    public void setScreenOn(boolean screenOn) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        if (app != null) {
            app.setScreenOn(screenOn);
        }
    }

    // 当前是否展示挑战福利
    @ReactMethod
    public void isRewarded(Promise promise) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        promise.resolve(app == null ? null : app.isRewarded());
    }

    // 设置是否展示挑战福利
    @ReactMethod
    public void setRewarded(boolean rewarded) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        if (app != null) {
            app.setRewarded(rewarded);
        }
    }

    // 是否二次确认退出
    @ReactMethod
    public void isQuitConfirm(Promise promise) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        promise.resolve(app == null ? null : app.isQuitGameConfirmFlag());
    }

    // 设置二次确认退出
    @ReactMethod
    public void setQuitConfirm(boolean confirm) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        if (app != null) {
            app.setQuitGameConfirmFlag(confirm);
        }
    }

    // 是否显示退出推荐
    @ReactMethod
    public void isQuitRecommend(Promise promise) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        promise.resolve(app == null ? null : app.isQuitGameConfirmRecommand());
    }

    // 显示退出推荐
    @ReactMethod
    public void setQuitRecommend(boolean recommend) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        if (app != null) {
            app.setQuitGameConfirmRecommand(recommend);
        }
    }

    // 获取退出时的提示词
    @ReactMethod
    public void getQuitConfirmTip(Promise promise) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        promise.resolve(app == null ? null : app.getQuitGameConfirmTip());
    }

    // 设置退出时的提示词
    @ReactMethod
    public void setQuitConfirmTip(String tip) {
        CmGameAppInfo app = CmGameSdk.getCmGameAppInfo();
        if (app != null) {
            app.setQuitGameConfirmTip(tip);
        }
    }

    // 获取所有游戏列表信息
    @ReactMethod
    public void getGameList(boolean withPlayNumbers, Promise promise) {
        List<GameInfo> gameInfo = CmGameSdk.getGameInfoList();
        promise.resolve(parseGameList(gameInfo, withPlayNumbers));
    }

    // 获取热门推荐游戏
    @ReactMethod
    public void getHotList(boolean withPlayNumbers, Promise promise) {
        List<GameInfo> gameInfo = CmGameSdk.getHotGameInfoList();
        promise.resolve(parseGameList(gameInfo, withPlayNumbers));
    }

    // 获取最近上新游戏
    @ReactMethod
    public void getNewList(boolean withPlayNumbers, Promise promise) {
        List<GameInfo> gameInfo = CmGameSdk.getNewGameInfoList();
        promise.resolve(parseGameList(gameInfo, withPlayNumbers));
    }

    // 获取最近3个常玩游戏
    @ReactMethod
    public void getLastPlayList(boolean withPlayNumbers, Promise promise) {
        List<GameInfo> gameInfo = CmGameSdk.getLastPlayGameInfoList();
        promise.resolve(parseGameList(gameInfo, withPlayNumbers));
    }

    // 由 gameId 获取单个游戏信息
    @ReactMethod
    public void getGameInfo(String gameId, boolean withPlayNumbers, Promise promise) {
        GameInfo gameInfo = CmGameSdk.getGameInfoByGameId(gameId);
        promise.resolve(parseGameInfo(gameInfo, withPlayNumbers));
    }

    // 由 gameId 获取在线人数
    @ReactMethod
    public void getPlayNumbers(String gameId, Promise promise) {
        promise.resolve(CmGameSdk.getGamePlayNumbers(gameId));
    }

    // 是否存在游戏
    @ReactMethod
    public void hasGame(String gameId, Promise promise) {
        promise.resolve(CmGameSdk.hasGame(gameId));
    }

    // 开始游戏
    @ReactMethod
    public void startGame(String gameId, Promise promise) {
        try {
            CmGameSdk.startH5Game(gameId);
            promise.resolve(true);
        } catch (Throwable e) {
            promise.reject(e);
        }
    }

    private WritableArray parseGameList(List<GameInfo> gameInfo, boolean withPlayNumbers) {
        WritableArray list = Arguments.createArray();
        if (gameInfo == null) {
            return list;
        }
        for (GameInfo game: gameInfo) {
            list.pushMap(parseGameInfo(game, withPlayNumbers));
        }
        return list;
    }

    private WritableMap parseGameInfo(GameInfo game, boolean withPlayNumbers) {
        if (game == null) {
            return null;
        }
        WritableArray typeTagList = Arguments.createArray();
        ArrayList<String> tags = game.getTypeTagList();
        for (String tag: tags) {
            typeTagList.pushString(tag);
        }
        WritableMap params = Arguments.createMap();
        params.putString("gameId", game.getGameId());
        params.putInt("gameIdServer", game.getGameIdServer());
        params.putString("name", game.getName());
        params.putString("iconUrl", game.getIconUrl());
        params.putBoolean("isNew", game.isNew());
        params.putBoolean("isRecommend", game.isRecommend());
        params.putBoolean("isBQGame", game.isBQGame());
        params.putBoolean("isLastPlayed", game.isLastPlayed());
        params.putBoolean("isHaveSetState", game.isHaveSetState());
        params.putInt("type", game.getType());
        params.putString("gameType", game.getGameType());
        params.putString("slogan", game.getSlogan());
        params.putString("iconUrlSquare", game.getIconUrlSquare());
        params.putArray("typeTagList", typeTagList);
        if (withPlayNumbers) {
            params.putInt("playNumbers", CmGameSdk.getGamePlayNumbers(game.getGameId()));
        }
        return params;
    }
}