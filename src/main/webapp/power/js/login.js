/***登录
 * yj
 */
var message;
var username;
var password;
/**
 * 校验参数
 */
function checkPsarameter() {
	var flag = true;
	message = "";
	username = $("#loginName").val();
	password = $("#loginPass").val();
	if(username.length == 0) {
		message += "用户名不能为空\r\n"
		flag = false;
	}

	if(password.length == 0) {
		message += "密码不能为空\r\n"
		flag = false;
	}
	return flag;
}

function login() {
	if(!checkPsarameter()) {
		alert(message);
		return false;
	}
	var url = serviceUrl;//
	var jsonParam = {
		"marked": "login",
		"jsonStr": ""
	};
	param = setJson(null, "username", username);
	param = setJson(param, "password", password);
	jsonParam.jsonStr = param;
	console.log(jsonParam);
	jQuery.axjsonp(url, jsonParam, callBack_login);
}

function callBack_login(data){
	console.log(data);
	if(data == undefined || data == null) {
		alert("登录接口返回数据为空");
		return;
	}
	if(data.rspCode != "000") {
		if(data.rspCode.indexOf('404') >= 0 || data.rspDesc.indexOf('404') >= 0) {
			alert('服务或页面异常，请稍后重试');
		} else {
			alert(data.rspDesc);
		}
	} else {
		var str = location.href; //取得整个地址栏
		var num = str.indexOf("?");
		if(num == -1){
			location.href = "http://127.0.0.1:8083/web-sso-all/index.html";
		}else{
			str = str.substr(num+1);
			var arr = str.split("=");
			location.href = arr[1];
		}
	}
}
