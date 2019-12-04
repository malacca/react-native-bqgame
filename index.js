import React, { PureComponent } from 'react';
import {
  Platform, 
  requireNativeComponent, 
  NativeModules, 
  DeviceEventEmitter, 
  NativeAppEventEmitter
} from 'react-native';
const { BqGameModule:bqgame } = NativeModules, IsAndroid = Platform.OS === 'android';

// 游戏中心组件
class BqGameCenter extends PureComponent {
  render(){
    const {children, ...props} = this.props;
    return (
      <RNBqGameView
        {...props}
      />
    );
  }
}
const RNBqGameView = requireNativeComponent('RNBqGameView', BqGameCenter);


// 初始化参数
const Attrs = [
  'appId',
  'appHost',
  'withX5',   // 若项目已集成x5, 通过 withX5(bool enable) 来决定是否在游戏中启用
              // 若未集成, 可通过 withX5(int level) 指定在 android api level 小于指定值时自动集成
  'account',  // 游戏账户 (需设置为 onLogin 回调产生的账户名)
  'mute',     // 是否静音
  'screenOn', // 是否在游戏时保持屏幕常亮
  'quitConfirm', // 是否在退出游戏时, 显示二次确认弹出框
  'quitRecommend', // 是否在退出弹出框中显示 推荐游戏
  'ttad',  // 广告配置
  /* ttad 的参数是一个 object, 设置广告相关参数
  {
      // rewardVideo:"936101115",  //激励视频
      // listFeed:"936101899",    //游戏列表广告 (信息流 + 自渲染)
      // listInteraction:"",  //显示游戏列表时, 先来一个插屏 (模板渲染1:1) 不建议设置这个
      // loadingInteraction:"936101814",  //游戏加载时显示 (插屏 1:1 + 模板渲染)
      // recommendFeed:"",   //退出游戏, 确认弹出框中显示的广告 (信息流 + 自渲染)

      // 下面3个是某些游戏会调用
      // fullVideo:"",  // 全屏视频
      // expressInteraction:"",  //插屏
      // expressBanner:"",   //模板渲染Banner 600*150

      // 下面是 listFeed 开启情况下的细节配置
      // adHot:true,  // 热门游戏下方是否显示广告 默认 true
      // adNew: true,  //最新游戏是否显示广告 默认 true
      // adMore:true,  //更多游戏 是否显示广告 默认 true
      // adMoreSlice:4,  //更多游戏 几排游戏 显示一个广告 默认4
  }
  */
];
const Events = [
  'onLogin',  // 登陆成功
  'onClick',  // 游戏被点击(开始游戏)
  'onBlur',  // 游戏页面失去焦点(可能是进入到广告或其他)
  'onFocus', // 游戏界面获得焦点
  'onPass', // 游戏内完成关卡回调 (回调参数中没有 gameId, 若有需要, 自行在 onClick 时记录)
  'onClose', // 结束游戏回调 (参数为 游戏时长)
  'onAd',  // 广告操作回调 {gameId:String, adType:int, adAction:int}
           // adType --- 1：激励视频广告；2：Banner广告；3：原生Banner广告；4：全屏视频广告；
                       //5：原生插屏广告；6：开屏大卡广告；7：模板Banner广告；8：模板插屏广告
           // adAction --- 1：曝光；2：点击；3：关闭；4：跳过
];
class Bus {
  init(){
    const initConfig = {};
    let onState = false, k, e, v;
    for (k in this) {
      e = k.substr(1);
      v = this[k];
      if (e === 'onBlur' || e === 'onFocus') {
        if (v) {
          onState = true;
        }
      } else if (e === 'withX5') {
        if (Number.isInteger(v)) {
          initConfig.withX5 = "auto";
          initConfig.maxLevel = Math.max(v, 17);
        } else {
          initConfig.withX5 = v ? "yes" : "no";
        }
      } else {
        initConfig[e] = Events.indexOf(e) === -1 ? v : (v ? true : false);
      }
    }
    initConfig.onState = onState;
    bqgame.initSdk(initConfig)
    return this;
  }
}
Attrs.concat(Events).forEach(k => {
  Object.defineProperty(Bus.prototype, k, {
    value:function(callback) {
      this['_'+k] = callback;
      return this
    }
  })
})

// 初始化方法:  config(id,host).ttad({ }).onClick(() => {}).init();
const sdkInitBus = new Bus();
const config = (id, host) => {
  const len = arguments.length;
  if (len > 1) {
    sdkInitBus.appId(id).appHost(host);
  } else if (len > 0) {
    sdkInitBus.appId(id)
  }
  return sdkInitBus;
}

// 处理回调
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
  key = '_' + key;
  sdkInitBus[key] && sdkInitBus[key](msg);
}
if(IsAndroid) {
    DeviceEventEmitter.addListener("BqGameEvent", listenBqGameEvent);
} else {
    NativeAppEventEmitter.addListener("BqGameEvent", listenBqGameEvent);
}

// 其他API 为预防 js 参数不对造成 crash, 这里提前过滤好参数, 再交给原生端
// 这么搞一下, 顺便可以让 api 可以使用链式方式调用
const jsapi = {
  config,
  BqGameCenter
}
jsapi.getInfo = () => {
  return bqgame.getInfo()
}
jsapi.setAccount = (account) => {
  account = account.trim();
  if (account) {
    bqgame.setAccount(account)
  }
  return jsapi
}
jsapi.clearAccount = () => {
  bqgame.clearAccount()
  return jsapi
}
jsapi.isMute = () => {
  return bqgame.isMute()
}
jsapi.setMute = (mute) => {
  bqgame.setMute(mute||mute === undefined)
  return jsapi
}
jsapi.isScreenOn = () => {
  return bqgame.isScreenOn()
}
jsapi.setScreenOn = (screenOn) => {
  setScreenOnNative(screenOn||screenOn === undefined)
  return jsapi
}
jsapi.setQuitConfirm = (confirm) => {
  setQuitConfirmNative(confirm||confirm === undefined)
  return jsapi
}
jsapi.setQuitRecommend = (recommend) => {
  setQuitRecommendNative(recommend||recommend === undefined)
  return this
}
jsapi.getGameList = (withPlayNumbers) => {
  return bqgame.getGameList(Boolean(withPlayNumbers))
}
jsapi.getHotList = (withPlayNumbers) => {
  return bqgame.getHotList(Boolean(withPlayNumbers))
}
jsapi.getNewList = (withPlayNumbers) => {
  return bqgame.getNewList(Boolean(withPlayNumbers))
}
jsapi.getLastPlayList = (withPlayNumbers) => {
  return bqgame.getLastPlayList(Boolean(withPlayNumbers))
}
jsapi.getGameInfo = (gameId, withPlayNumbers) => {
  gameId = gameId.trim();
  return gameId ? bqgame.getGameInfo(gameId, Boolean(withPlayNumbers)) : new Promise((resolve) => {
    resolve(null)
  })
}
jsapi.hasGame = (gameId) => {
  gameId = gameId.trim();
  return gameId ? bqgame.hasGame(gameId) : new Promise((resolve) => {
    resolve(false)
  })
}
jsapi.startGame = (gameId) => {
  gameId = gameId.trim();
  if (gameId) {
    return bqgame.startGame(gameId)
  }
  return new Promise((resolve, reject) => {
    reject("game id is empty")
  })
}


module.exports = jsapi;