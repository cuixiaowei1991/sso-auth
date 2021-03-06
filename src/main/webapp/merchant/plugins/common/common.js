//服务地址

//登录跳转首页地址
//测试
var indexHtml='http://192.168.15.154:8380/index.html';
var merchantLoginUrl = 'http://192.168.15.155:8280/sso-auth-service/merchant';
//生产
//var indexHtml='http://192.168.20.105:8380/index.html';
//单点登录接口地址var merchantLoginUrl = 'http://192.168.15.155:8280/sso-auth-service/merchant';
//生产
//var merchantLoginUrl = 'http://192.168.20.102:8181/sso-auth-service/merchant';
$(function() {
	/**
	 * ajax封装
	 * Jsonp格式，用于跨域调用
	 * url 发送请求的地址
	 * data 发送到服务器的数据，数组存储，如：{"date": new Date().getTime(), "state": 1}
	 * successfn 成功回调函数
	 */
	jQuery.axjsonp = function(url, data, successfn, config) {
		data = (data == null || data == "" || typeof(data) == "undefined") ? {
			"date": new Date().getTime()
		} : data;
		$.ajax({

			//提交数据的类型 POST GET
			type: "POST",
			//async:false,  
			//提交的网址
			url: url,
			//提交的数据
			data: data,
			//返回数据的格式
			dataType: "jsonp", //"xml", "html", "script", "json", "jsonp", "text".
			jsonp: "callbackparam", //传递给请求处理程序或页面的，用以获得jsonp回调函数名的参数名(默认为:callback)
			jsonpCallback: "success_jsonpCallback", //自定义的jsonp回调函数名称，默认为jQuery自动生成的随机函数名
			//在请求之前调用的函数
			beforeSend: function() {
				$("#btn_loading").css('display', '');
			},
			//成功返回之后调用的函数             
			success: function(data) {
				if(config != null && config != undefined && config != '') {
					successfn(data, config);
				} else {
					successfn(data);
				}
			},
			//调用执行后调用的函数
			complete: function(XMLHttpRequest, textStatus) {
				$("#btn_loading").css('display', 'none');
			},
			//调用出错执行的函数
			error: function(XMLHttpRequest, textStatus, errorThrown) {
				console.log(XMLHttpRequest.status);
				console.log(textStatus);
				//				console.log(textStatus);
				if(XMLHttpRequest.status != '200') {
					alert("AJAX请求错误：请求状态码：" + XMLHttpRequest.status + " readyState:" + XMLHttpRequest.readyState + "错误：" + textStatus);
				}
			}
		});
	};

	jQuery.axjsonpPost = function(url, data, successfn) {
		data = (data == null || data == "" || typeof(data) == "undefined") ? {
			"date": new Date().getTime()
		} : data;
		$.ajax({
			crossDomain: true,
			//提交数据的类型 POST GET
			type: "POST",
			//async:false,  
			//提交的网址
			url: url,
			//提交的数据
			data: data,
			//返回数据的格式
			dataType: "json", //"xml", "html", "script", "json", "jsonp", "text".
			//jsonp: "callbackparam", //传递给请求处理程序或页面的，用以获得jsonp回调函数名的参数名(默认为:callback)
			//jsonpCallback: "success_jsonpCallback", //自定义的jsonp回调函数名称，默认为jQuery自动生成的随机函数名
			//在请求之前调用的函数
			beforeSend: function() {
				$("#btn_loading").css('display', '');
			},
			//成功返回之后调用的函数             
			success: function(data) {

				successfn(data);
			},
			//调用执行后调用的函数
			complete: function(XMLHttpRequest, textStatus) {
				$("#btn_loading").css('display', 'none');
			},
			//调用出错执行的函数
			error: function(XMLHttpRequest, textStatus, errorThrown) {
				console.log(XMLHttpRequest.status);
				console.log(textStatus);
				//				console.log(textStatus);
				if(XMLHttpRequest.status != '200') {
					alert("AJAX请求错误：请求状态码：" + XMLHttpRequest.status + " readyState:" + XMLHttpRequest.readyState + "错误：" + textStatus);
				}
			}
		});
	};

	jQuery.axjsonpPostMvc = function(url, data, successfn, config, key) {
		data = (data == null || data == "" || typeof(data) == "undefined") ? {
			"date": new Date().getTime()
		} : data;
		$.ajax({
			type: "POST",
			url: url,
			data: JSON.stringify(data),
			headers: {
				'Content-Type': 'application/json',
				'TestKey': key
			},
			async: false,
			//在请求之前调用的函数
			beforeSend: function() {
				$("#btn_loading").css('display', '');
			},
			//成功返回之后调用的函数             
			success: function(data) {
				if(config != null && config != undefined && config != '') {
					successfn(data, config);
				} else {
					successfn(data);
				}
			},
			//调用执行后调用的函数
			complete: function(XMLHttpRequest, textStatus) {
				$("#btn_loading").css('display', 'none');
			},
			//调用出错执行的函数
			error: function(XMLHttpRequest, textStatus, errorThrown) {
				console.log(XMLHttpRequest.status);
				console.log(textStatus);
				//				console.log(textStatus);
				if(XMLHttpRequest.status != '200') {
					alert("AJAX请求错误：请求状态码：" + XMLHttpRequest.status + " readyState:" + XMLHttpRequest.readyState + "错误：" + textStatus);
				}
			}
		});
	};

});