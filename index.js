import React, { PureComponent } from 'react';
import {
  Platform, 
  requireNativeComponent, 
  NativeModules, 
  DeviceEventEmitter, 
  NativeAppEventEmitter
} from 'react-native';
const { BqGameModule } = NativeModules, IsAndroid = Platform.OS === 'android';

// 游戏中心组件
let onGameCenterReady = null;
class BqGameCenter extends PureComponent {
  render(){
    const {children, onLoad, ...props} = this.props;
    onGameCenterReady = onLoad;
    return (
      <RNBqGameView
        {...props}
      />
    );
  }
}
const RNBqGameView = requireNativeComponent('RNBqGameView', BqGameCenter);

// 导出API
const jsapi = {
  BqGameCenter
};

// 支持的回调
const _binds = {};
const _events = [
  'onLogin',
  'onListReady',
  'onClick',
  'onBlur',
  'onFocus',
  'onPass',
  'onClose',
  'onAd'
];
/*
1. 初始化 SDK, 支持所有参数, 必须设置 appId/appHost
2. appId/appHost/debug/withX5/ttad/回调函数 只能在初始化时设置, 其他参数后期仍可设置
config = {
  'appId',
  'appHost',
  'account',  // 游戏账户 (需设置为 onLogin 回调产生的账户名)
  'debug',   // 是否开启 android 日志, debug 模式默认为 true, 反之 false
  'withX5',   // withX5(true)  - 已在外部集成x5, 告知即可
              // withX5(false) - 不启用 x5
              // withX5(null)  - 尝试自动集成 x5
              // 可自行判断, 比如判断 android api level 小于某个值自动集成, 否则不启用

  'mute',     // 是否静音
  'screenOn', // 是否在游戏时保持屏幕常亮
  'showVip',  // 是否显示 VIP
  'rewarded', // 是否开启福利
  'quitConfirm', // 是否在退出游戏时, 显示二次确认弹出框
  'quitRecommend', // 是否在退出弹出框中显示 推荐游戏
  'quitConfirmTip', // 退出时的提示语

  // 广告配置
  'ttad': {
    // listInteraction:"",  //显示游戏列表时, 先来一个插屏 (模板渲染1:1) 不建议设置这个

    // listFeed:"",    //游戏列表广告 (信息流 + 自渲染)
    // listExpress:"",    //游戏列表广告 (信息流 + 模板渲染), 与 listFeed 二选一

    // loadingNative:""   // 游戏加载时显示 (插屏 + 自渲染)
    // loadingInteraction:"",  //游戏加载时显示 (插屏 1:1 + 模板渲染), 与 loadingNative 二选一

    // rewardVideo:"",  // 游戏内 激励视频
    // expressBanner:"",   // 部分游戏内Banner广告，(模板渲染，尺寸：600*150)
    // fullVideo:"",  // 部分游戏 全屏视频
    // expressInteraction:"",  //部分游戏内插屏

    // endFeed:"",   //退出游戏, 确认弹出框中显示的广告 (信息流 + 自渲染)
    // endExpress:"",   //退出游戏, 确认弹出框中显示的广告 (信息流 + 模板渲染) 与 endFeed 二选一

    // 下面是 listFeed 开启情况下的细节配置
    // adHot:true,  // 热门游戏下方是否显示广告 默认 true
    // adNew: true,  //最新游戏是否显示广告 默认 true
    // adMore:true,  //更多游戏 是否显示广告 默认 true
    // adMoreSlice:4,  //更多游戏 几排游戏 显示一个广告 默认4
  },

  // 回调相关
  'onLogin',  // 登陆成功
  'onListReady',  // 列表加载成功后 (同时会触发 BqGameCenter 组件的 onLoad 回调)
  'onClick',  // 游戏被点击(开始游戏)
  'onBlur',  // 游戏页面失去焦点(可能是进入到广告或其他)
  'onFocus', // 游戏界面获得焦点
  'onPass', // 游戏内完成关卡回调 (回调参数中没有 gameId, 若有需要, 自行在 onClick 时记录)
  'onClose', // 结束游戏回调 (参数为 游戏时长)
  'onAd',  // 广告操作回调 {gameId:String, adType:int, adAction:int}
           // adType --- 1：激励视频广告；2：Banner广告；3：原生Banner广告；4：全屏视频广告；
                       //5：原生插屏广告；6：开屏大卡广告；7：模板Banner广告；8：模板插屏广告
           // adAction --- 1：曝光；2：点击；3：关闭；4：跳过
}
*/
jsapi.initSdk = (config) => {
  let k, v, format = {};
  for (k in config) {
    v = config[k];
    if (_events.includes(k)) {
      _binds[k] = v;
      if (k === 'onBlur' || k === 'onFocus') {
        format.onState = true;
      } else {
        format[k] = true;
      }
    } else if (k === 'withX5') {
      format.withX5 = v === true ? "yes" : (v === false ? "no" : "auto");
    } else {
      format[k] = v;
    }
  }
  // 该项必须绑定
  format.onListReady = true;
  BqGameModule.initSdk(format);
  return jsapi;
}
// Bool 类型配置的 设置与获取
// 获取返回 Promise, 设置返回 jsapi, 支持链式调用
['Mute', 'ScreenOn',  'ShowVip', 'Rewarded', 'QuitConfirm', 'QuitRecommend'].forEach(k => {
  const get = 'is'+k, set = 'set'+k;
  jsapi[get] = () => {
    return BqGameModule[get]();
  }
  jsapi[set] = (bool) => {
    BqGameModule[set](bool === undefined||Boolean(bool));
    return jsapi;
  }
});
// 其他可用 API
jsapi.isX5 = () => {
  return BqGameModule.isX5()
}
// 若自行渲染列表, 需调用该函数登录
jsapi.initAccount = () => {
  BqGameModule.initAccount();
  return jsapi;
}
jsapi.setAccount = (account) => {
  account = account.trim();
  if (account) {
    BqGameModule.setAccount(account)
  }
  return jsapi
}
jsapi.clearAccount = () => {
  BqGameModule.clearAccount();
  return jsapi;
}
jsapi.getGameList = (withPlayNumbers) => {
  return BqGameModule.getGameList(Boolean(withPlayNumbers))
}
jsapi.getHotList = (withPlayNumbers) => {
  return BqGameModule.getHotList(Boolean(withPlayNumbers))
}
jsapi.getNewList = (withPlayNumbers) => {
  return BqGameModule.getNewList(Boolean(withPlayNumbers))
}
jsapi.getLastPlayList = (withPlayNumbers) => {
  return BqGameModule.getLastPlayList(Boolean(withPlayNumbers))
}
jsapi.getGameInfo = (gameId, withPlayNumbers) => {
  gameId = gameId.trim();
  return gameId ? BqGameModule.getGameInfo(gameId, Boolean(withPlayNumbers)) : new Promise((resolve) => {
    resolve(null)
  })
}
jsapi.hasGame = (gameId) => {
  gameId = gameId.trim();
  return gameId ? BqGameModule.hasGame(gameId) : new Promise((resolve) => {
    resolve(false)
  })
}
jsapi.startGame = (gameId) => {
  gameId = gameId.trim();
  if (gameId) {
    return BqGameModule.startGame(gameId)
  }
  return new Promise((resolve, reject) => {
    reject("game id is empty")
  })
}

// 监听原生端消息
const listenBqGameEvent = e => {
  let key, {event, ...msg} = e;
  if (event === 'onState') {
    const {state} = msg;
    key = state > 1 ? 'onFocus' : 'onBlur';
    msg = undefined;
  } else {
    key = event;
    if (key === 'onPass') {
      const {info} = msg;
      try {
        msg = JSON.parse(info)
      } catch (e) {
        // do nothing
      }
    }
  }
  if (key === 'onListReady' && onGameCenterReady) {
    onGameCenterReady();
    onGameCenterReady = null;
  }
  _binds[key] && _binds[key](msg);
}
if(IsAndroid) {
    DeviceEventEmitter.addListener("BqGameEvent", listenBqGameEvent);
} else {
    NativeAppEventEmitter.addListener("BqGameEvent", listenBqGameEvent);
}

module.exports = jsapi;