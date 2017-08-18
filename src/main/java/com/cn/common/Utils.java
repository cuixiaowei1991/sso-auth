package com.cn.common;

import org.json.JSONObject;


/**
 * 
 * @author songzhili
 * 2016年11月2日上午10:18:30
 */
public class Utils {
   
	/**
	 * 
	 * @param source
	 * @return
	 */
	public static boolean isEmpty(String source){
		
		if(source == null || source.trim().length() == 0){
			return true;
		}
		return false;
	}
	/**
	 * 
	 * @param source
	 * @return
	 */
    public static boolean isNullOrEmpty(Object source){
		
		if(source == null || source.toString().trim().length() == 0
				|| "null".equals(source)){
			return true;
		}
		return false;
	}
    /**
     * 
     * @param source
     * @param source1
     * @return
     */
	public static boolean areEmpty(String source,String source1){
		
		if(isEmpty(source) || isEmpty(source1)){
			return true;
		}
		return false;
	}
	/**
	 * 
	 * @param source
	 * @return
	 */
	public static String encrypt(String source,String secretKey){
		return DESUtils.encrypt(source, secretKey);
	}
	
	public static String decrypt(String source,String secretKey){
		
		boolean hasException = false;
		try {
			if(source.startsWith("prefix")){
				source = source.substring(7, source.length());
				return DESUtils.decrypt(source, secretKey);
			}
		} catch (Exception ex) {
			hasException = true;
			ex.printStackTrace();
		}
		if(hasException){
			return "exception";
		}
		source = MD5Helper.md5(source, "UTF-8");
		return source;
	}
	/**
	 * 
	 * @param source
	 * @return
	 */
	public static String removeTimeOut(String source){
		
		JSONObject obj = new JSONObject(source);
		if(!isNullOrEmpty(obj.get("ticketTimeout"))){
			obj.remove("ticketTimeout");
		}
		return obj.toString();
	}
	
	public static void main(String[] args) {
		
		String ss = "prefix_FC5575D3A449DE850C913D3E54D20108D9D6E36C395A7C5045CF1735FB12D59BAD6A88B4FA37833D";
		System.out.println(ss.length());
		System.out.println(decrypt(ss, "111111112222222233333333"));
	}
}
