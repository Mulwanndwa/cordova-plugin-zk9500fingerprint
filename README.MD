Note: This plugin allows for easy cordova integration with the zkteco ZK9500 fingerprint reader

## SDK Reference:
**See**: https://zkteco.eu/downloads/zkfinger-sdk-v100-zk9500-usb-reader

## Install

```
cordova plugin add https://github.com/Mulwanndwa/cordova-plugin-zk9500
```
Don't forget to add `android.hardware.usb.host` feature to the `AndroidManifest.xml`.
```xml
 <meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" android:resource="@xml/device_filter" />
<uses-feature android:name="android.hardware.usb.host" />
<uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
```

```js
cordova.plugins.zkFinger.scan(function(success){
    console.log("success",success);
}, function(error){
    console.log("error",error);
});

## Licence ##
Copyright (c) 2025 Mpho Mulwanndwa (South Africa)
