// INettyServiceCallback.aidl
package com.tianfy.nettylib;

interface INettyServiceCallback {

  void receiveBytes(in byte[] bytes,in String ip,in int port);

}