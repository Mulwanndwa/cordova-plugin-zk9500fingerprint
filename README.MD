Note: This plugin allows for easy cordova integration with the zkteco ZK9500 fingerprint reader

## SDK Reference:
**See**: https://zkteco.eu/downloads/zkfinger-sdk-v100-zk9500-usb-reader

## Install

```
cordova plugin add https://github.com/Mulwanndwa/cordova-plugin-zk9500
```

```js
cordova.plugins.zkFinger.scan(function(success){
    console.log("success",success);
}, function(error){
    console.log("error",error);
});

