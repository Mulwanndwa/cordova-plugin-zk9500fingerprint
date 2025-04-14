Note: This plugin allows for easy cordova integration with the zkteco ZK9500 fingerprint reader

## SDK Reference:
**See**: https://zkteco.eu/downloads/zkfinger-sdk-v100-zk9500-usb-reader

## Install

```
cordova plugin add https://github.com/Mulwanndwa/cordova-plugin-zktecoFinger
```
Don't forget to add `android.hardware.usb.host` feature to the `AndroidManifest.xml`.
```xml
 <meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" android:resource="@xml/device_filter" />
<uses-feature android:name="android.hardware.usb.host" />
<uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
```

```js
cordova.plugins.zktecoFinger.scan(function(success){
    console.log("success",success);
}, function(error){
    console.log("error",error);
});

## Licence ##
MIT License

Copyright (c) 2025 Mpho Mulwanndwa

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
