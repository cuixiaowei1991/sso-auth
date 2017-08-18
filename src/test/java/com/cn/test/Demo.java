package com.cn.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import com.cn.common.WebUtils;


/**
 * 
 * @author songzhili
 * 2017年1月9日上午10:27:03
 */
public class Demo {
   
	@Test
	public void test(){
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		String url = "http://192.168.15.155:8280/sso-auth-service/merchant";
		node.put("marked", "logout");
		ObjectNode nodeOne = mapper.createObjectNode();
		nodeOne.put("ticket", "E8D35BD34BD774A0031AEFFF1DA6636294F7791ABCC5C5860B620F277AB361DBAD6A88B4FA37833D");
		node.put("jsonStr", nodeOne);
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("marked", "logout");
		map.put("jsonStr", nodeOne.toString());
		try {
//			byte [] content = node.toString().getBytes("UTF-8");
//			String ctype = "application/json;charset=UTF-8";
			String responseData = WebUtils.doPost(url, map, 50000, 80000);
			System.out.println(responseData);
		} catch (IOException e) {
		}
	}
	
	@Test
	public void test00() throws UnsupportedEncodingException{
		String ss = "%7B%22nick%22%3A%22cuixiaowei%22%2C%22userId%22%3A%22ff8081815987b80e015987bba84a0006%22%2C%22userName%22%3A%22cui%22%2C%22password%22%3A%22111111%22%7D";
		ss = URLDecoder.decode(ss, "UTF-8");
		System.out.println(ss);
	}
	
	@Test
	public void tesdd(){
		String ss = "sdfsdfsd";
		int index  = ss.indexOf("?");
		System.out.println(index);
	}
}















