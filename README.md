# react-native-bqgame
react native bqgame

# 安装

`yarn add react-native-bqgame`


# Android 配置

## 1.`android/app/src/main/androidManifest.xml`

```
<manifest ..>

<!-- 广告需要使用 http client 在 Android 9.0 会 class not found 而加的配置 -->
<uses-library android:name="org.apache.http.legacy" android:required="false" />

</manifest>
```

## 2.依赖冲突 / 启用X5

参阅 [build.gradle](android/build.gradle#L30) 解决以下问题

- 若编译过程，相关依赖版本冲突
- x5 内核集成的相关设置


# iOS 配置

还未开发 iOS 版本，TODO



# 使用

``` js
import {Platform} from 'react-native';
import bqgame, {BqGameCenter} from 'react-native-bqgame'

const x5 = Platform.OS === 'android' && Platform.Version < 22 ? null : false;

// 初始化 链式设置参数
bqgame.config(appId, appHost).quitConfirm(true).ttad({
    rewardVideo:""
}).onLogin(account => {

}).onClick(() => {

}).withX5(x5).init();

// 初始化之后 使用组件载入游戏列表
<BqGameCenter />
```

链式过程可使用的函数参见：[index.js](index.js#L26)



## 更多API 

```js

// 设置游戏账户, 需使用 onLogin 回调得到的 account
bqgame.setAccount(String account)

// 退出当前账户
bqgame.clearAccount()

// 是否启用x5
bqgame.isX5().then((Boolean x5) => { })

// 设置是否静音
bqgame.mute(Boolean mute)

// 是否静音
bqgame.isMute().then((Boolean mute) => { })

// 设置游戏时是否保持屏幕常亮
bqgame.setScreenOn(Boolean mute)

// 是否屏幕常亮
bqgame.isScreenOn().then((Boolean screenOn) => { })

// 设置退出游戏 是否显示二次确认框
bqgame.setQuitConfirm(Boolean yes);

// 设置退出游戏 是否显示推荐游戏
bqgame.setQuitRecommend(Boolean yes);

// 所有游戏列表
bqgame.getGameList().then( lists => { } )

// 热门推荐游戏列表
bqgame.getHotList().then( lists => { } )

// 最近上新游戏列表
bqgame.getNewList().then( lists => { } )

// 获取最近3个常玩游戏
bqgame.getLastPlayList().then( lists => { } )

// 由 gameId 获取单个游戏信息
bqgame.getLastPlayList(String gameId).then( info => { } )

// 由 gameId 获取在线人数
bqgame.getPlayNumbers(String gameId).then( numbers => { } )

// 游戏是否存在
bqgame.hasGame(String gameId).then( has => { } )

// 开始游戏
bqgame.startGame(String gameId)
```

