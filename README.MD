This plugin allows for easy cordova integration with the zkteco ZK9500 fingerprint reader

https://github.com/Mulwanndwa/cordova-plugin-zkfinger
<script>
cordova.plugins.zkFinger.scan(function(success){
    console.log("success",success);
}, function(error){
    console.log("error",error);
});
</script>
