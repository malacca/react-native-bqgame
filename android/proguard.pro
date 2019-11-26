# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/sumirrowu/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# 需要维持拓展库中的注释
-keep class com.cmgame.x5fit.X5CmGameSdk{*;}
-keep class com.cmgame.x5fit.X5WebViewModule{*;}

# 需要保持游戏SDK的Bean类不被混淆
-keep class com.cmcm.cmgame.gamedata.bean.* {*;}
-keep class com.cmcm.cmgame.bean.* {*;}
-keep class com.cmcm.cmgame.httpengine.bean.* {*;}