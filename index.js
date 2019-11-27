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


// 相关API
const {initSdk, setMute:NativeSetMute, setScreenOn:NativeSetScreenOn, ...nativeAPI} =  BqGameModule;
const setMute = (mute) => {
  NativeSetMute(mute||mute === undefined)
}
const setScreenOn = (screenOn) => {
  NativeSetScreenOn(screenOn||screenOn === undefined)
}

// 初始化参数
const Attrs = [
  'appId',
  'appHost',
  'account',  // 游戏账户 (需设置为 onLogin 回调产生的账户名)
  'mute',     // 是否静音
  'screenOn', // 是否在游戏时保持屏幕常亮
  'quitConfirm', // 是否在退出游戏时, 显示二次确认弹出框
  'quitRecommend', // 是否在退出弹出框中显示 推荐游戏
  'ttad',  // 广告配置
  // {
  //   rewardVideo: # 激励视频
  //   listFeed： # 游戏列表广告 (信息流 + 自渲染)
  //   listInteraction: # 显示游戏列表时, 先来一个插屏 (模板渲染1:1) 不建议设置这个
  //   loadingInteraction:  # 游戏加载时显示 (插屏 1:1 + 模板渲染)
  //   loadingNative:  # 与loadingInteraction二选一 (插屏 + 原生 + 自渲染 + 大图, 该类型广告 新账户已无法申请)
  //   recommendFeed:  # 退出游戏, 确认弹出框中显示的广告 (信息流 + 自渲染)

  //   # 下面3个是某些游戏会调用
  //   fullVideo: # 全屏视频
  //   expressInteraction: # 插屏
  //   expressBanner:  # 模板渲染Banner 600*150

  //   # 下面是 listFeed 开启情况下的细节配置
  //   adHot:true,  # 热门游戏下方是否显示广告
  //   adNew: true, # 最新游戏是否显示广告
  //   adMore:true, # 更多游戏 是否显示广告
  //   adMoreSlice:4, # 更多游戏 几排游戏 显示一个广告
  // }
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
      } else {
        initConfig[e] = Events.indexOf(e) === -1 ? v : (v ? true : false);
      }
    }
    initConfig.onState = onState;
    initSdk(initConfig)
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

module.exports = {
  ...nativeAPI,
  setMute,
  setScreenOn,
  config,
  BqGameCenter
}