import React, { PureComponent } from 'react';
import {
  Platform, 
  requireNativeComponent, 
  NativeModules, 
  DeviceEventEmitter, 
  NativeAppEventEmitter
} from 'react-native';
const { BqGameModule } = NativeModules, IsAndroid = Platform.OS === 'android';

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

module.exports = {
  ...BqGameModule,
  BqGameCenter
}
