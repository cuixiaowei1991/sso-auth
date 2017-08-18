package com.cn.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.cn.redis.RedisClient;

/**
 * 
 * @author songzhili
 * 2016年11月1日下午4:59:00
 */
public class RecoverTicket implements Runnable {
	
	private final RedisClient redisClient;
	
	public RecoverTicket(RedisClient redisClient) {
		super();
		this.redisClient = redisClient;
	}

	@Override
	public void run() {
		
		Iterator<String> iterator = redisClient.keyIterator();
		List<String> keyList = new ArrayList<String>();
		while(iterator.hasNext()){
			String key = iterator.next();
			if(!"business".equals(key)
					&& !"power".equals(key)
					&& !"merchant".equals(key)){
			    keyList.add(key);
			}
		}
		/****/
		if(!keyList.isEmpty()){
			for(String key : keyList){
				String timeAndName = redisClient.getTicket(key);
				Map<String, Object> map = parseSourceJson(timeAndName);
				if(map != null){
					if(map.containsKey("ticketTimeout")){
						String expireTime = map.get("ticketTimeout").toString();
						long expireTimeOne = Long.parseLong(expireTime);
						if(expireTimeOne <= System.currentTimeMillis()){
							redisClient.deleteTicket(key);
						}
					}
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String,Object> parseSourceJson(String source){
		
		if(!source.endsWith("}")){
			return null;
		}
		JSONReader jsonReader = new JSONReader();
		Object obj = jsonReader.read(source);
		if(obj instanceof Map<?, ?>){
			return (Map<String, Object>)obj;
		}
		return null;
	}
}
