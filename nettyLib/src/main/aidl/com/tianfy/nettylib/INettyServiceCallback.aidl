// INettyServiceCallback.aidl
package com.tianfy.nettylib;

interface INettyServiceCallback {

  void receiveBytes(in byte[] bytes);

}