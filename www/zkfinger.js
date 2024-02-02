var exec = require('cordova/exec');

//@ enroll a fingerprint
exports.scan = function(success,error)
{
    exec(success,error,"zkFinger","scan",[]);
}

exports.custom = function(filePath,fileContent,success,error)
{    
    exec(success,error,"zkFinger","saveTemplate", [filePath,fileContent]);
}

// function success(data)
// {
// 	alert(JSON.stringify(data));
// }

// function error(errorMessage)
// {
// 	alert(JSON.stringify(errorMessage));
// }
