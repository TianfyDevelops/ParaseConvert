// INettyInterface.aidl
package com.tianfy.nettylib;

// Declare any non-default types here with import statements
import com.tianfy.nettylib.INettyServiceCallback;
interface INettyInterface {

    void addCallback(in INettyServiceCallback callback);

    void removeCallback(in INettyServiceCallback callback);
}