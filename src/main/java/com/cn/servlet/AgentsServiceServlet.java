package com.cn.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.cn.common.DESUtils;
import com.cn.common.JSONReader;
import com.cn.common.LogHelper;
import com.cn.common.Utils;
import com.cn.jdbc.AgentsJdbcClient;
import com.cn.redis.RedisClient;
/**
 * 代理商　单点登录授权中心
 * @author songzhili
 * 2017年1月13日下午5:51:06
 */
public class AgentsServiceServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7054743152357053505L;
	
	/** 秘钥cookie名称 */
	private String cookieName;
	/****/
	private String userCookieName;
	/** 是否安全协议 */
	private boolean secure;
	/** 密钥 */
	private String secretKey;
	/** ticket有效时间 */
	private int ticketTimeout;
	/****/
	private String dataBaseFile;
	/****/
	private String redisFile;
	/****/
	private RedisClient redisClient;
	/****/
	private AgentsJdbcClient jdbcClient;
	
	public void init(ServletConfig config)throws ServletException{
		this.cookieName = config.getInitParameter("cookieName");
		this.userCookieName = config.getInitParameter("userCookieName");
		this.secure = Boolean.parseBoolean(config.getInitParameter("secure"));
		this.secretKey = config.getInitParameter("secretKey");
		this.ticketTimeout = Integer.parseInt(config.getInitParameter("ticketTimeout"));
		this.dataBaseFile = config.getInitParameter("dataBaseFile");
		this.redisFile = config.getInitParameter("redisFile");
		this.redisClient = RedisClient.getRedisClient(this.redisFile);
		this.jdbcClient = new AgentsJdbcClient(this.dataBaseFile);
	}
	
	public void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException,IOException{
		
		HttpServletRequest request = (HttpServletRequest)req;
		HttpServletResponse response = (HttpServletResponse)resp;
		String marked = request.getParameter("marked");
		Cookie ticket = getCookieFromRequest(request,this.cookieName);
		String jsonStr = request.getParameter("jsonStr");
		if("beforeLogin".equals(marked)){//校验
			beforeLogin(request, response,ticket);
		}else if("login".equals(marked)) {//登录
			doLogin(request, response,jsonStr,ticket);
		} else if("logout".equals(marked)) {//注销登录
			doLogout(request, response,ticket,jsonStr);
		} else if("checkUpTicket".equals(marked)) {//cookie有效性校验
			checkUpTicket(request, response,jsonStr);
		}else if("loginFromBusiness".equals(marked)){//从业务系统跳转到代理商系统
			loginFromBusiness(request, response, jsonStr, ticket);
		}else{//直接跳转到登录页面
			String loginPageUrl = obtainLoginPageUrl(request);
			response.sendRedirect(loginPageUrl);
		}
	}
	
	 /**
     * 
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
	private void beforeLogin(HttpServletRequest request, HttpServletResponse response,Cookie ticket)
			throws ServletException, IOException {
		
		String setCookieURL = request.getParameter("setCookieURL");
		String gotoURL = request.getParameter("gotoURL");
		/*****/
		String loginPageUrl = obtainLoginPageUrl(request);
		StringBuilder urlNew = new StringBuilder();
		urlNew.append(loginPageUrl).append("?gotoURL=").append(gotoURL);
		if(ticket == null) {//名为SSOID的cookie为空直接跳转至登录页面
			LogHelper.info("^^^^^^代理商系统^^^^^^^跳转到首页gotoURL"+urlNew);
			response.sendRedirect(urlNew.toString());
		}else {//名为SSOID的cookie不为空
			LogHelper.info("^^^^^^代理商系统^^^^^^^跳转到首页setCookieURL:"+setCookieURL);
			String encodedTicket = ticket.getValue();
			String decodedTicket = DESUtils.decrypt(encodedTicket, this.secretKey);
			String ticketTime = this.redisClient.getTicket(decodedTicket);
			if(ticketTime != null) {//校验cookie的有效性
				int expiry = checkExpiryTime(ticketTime);
				if(expiry > 0){
					 if(setCookieURL != null){//setCookieURL 不为空
						StringBuilder together = new StringBuilder(setCookieURL);
						together.append("?ticket=").append(encodedTicket);
						together.append("&expiry=").append(expiry);
						together.append("&gotoURL=").append(gotoURL);
						ticketTime = Utils.removeTimeOut(ticketTime);
						ticketTime = URLEncoder.encode(ticketTime, "UTF-8");
						ticketTime = URLEncoder.encode(ticketTime, "UTF-8");
						together.append("&userCookie=").append(ticketTime);
						response.sendRedirect(together.toString());
					 }else{//setCookieURL 为空 跳转至登录页面
						 response.sendRedirect(urlNew.toString());
					 }
				}else{
					response.sendRedirect(urlNew.toString());
				}
			} else {//无效的cookie
				response.sendRedirect(urlNew.toString());
			}
		}
	}
    /**
     * 校验cookie的有效性
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException 
     */
	private void checkUpTicket(HttpServletRequest request, HttpServletResponse response,
			String data) 
			throws IOException, ServletException {
		
		LogHelper.info("^^^^^^代理商系统^^^^^^^校验cookie有效性"+data);
		Map<String, Object> jsonStr = parseSourceJson(data);
		/****/
		String loginPageUrl = obtainLoginPageUrl(request);
		if(jsonStr.get("cookieValue") == null) {
			request.getRequestDispatcher(loginPageUrl).forward(request, response);
		} else {
			JSONObject json = new JSONObject();
			String encodedTicket = jsonStr.get("cookieValue").toString();
			String decodedTicket = DESUtils.decrypt(encodedTicket, secretKey);
			String ticketMessage = redisClient.getTicket(decodedTicket);
			if(ticketMessage != null){
				Map<String, Object> map = parseSourceJson(ticketMessage);
				String expireTime = map.get("ticketTimeout").toString();
				long expireTimeOne = Long.parseLong(expireTime);
				if(expireTimeOne > System.currentTimeMillis()){
					redisClient.updateTicket(decodedTicket, obtainNewExpiryTime(map,this.ticketTimeout,false));
					json.put("rspCode", "000");
					json.put("rspDesc", "成功!!!");
				}else{
					json.put("rspCode", "001");
					json.put("rspDesc", "cookie无效");
				}
			}else{
				json.put("rspCode", "002");
				json.put("rspDesc", "cookie无效");
			}
			response.setContentType("text/json;charset=utf-8");
			PrintWriter out = response.getWriter();
			out.write(json.toString());
			out.close();
		}
	}
    /**
     * 
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException 
     */
	private void doLogout(HttpServletRequest request, HttpServletResponse response,
			Cookie ticket,String jsonStr)
			throws IOException, ServletException {

		//String loginPageUrl = obtainLoginPageUrl(request);
		JSONObject json = new JSONObject();
		if(jsonStr != null){
			Map<String, Object> data = parseSourceJson(jsonStr);
			if(!Utils.isNullOrEmpty(data.get("ssoCookie"))){
				String encodedTicket = data.get("ssoCookie").toString();
				String decodedTicket = DESUtils.decrypt(encodedTicket, this.secretKey);
				LogHelper.info("^^^^^代理商系统^^^^^^^退出系统decodedTicket:"+decodedTicket);
				String ticketTime = this.redisClient.getTicket(decodedTicket);
				if(ticketTime != null){
					redisClient.deleteTicket(decodedTicket);
				}
			}
			json.put("rspCode", "000");
			json.put("rspDesc", "");
		}else{
			LogHelper.info("^^^^^代理商系统^^^^^^^退出系统====================================>");
			json.put("rspCode", "000");
			json.put("rspDesc", "");
		}
		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		out.close();
	}
    /**
     * 
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
	private void doLogin(HttpServletRequest request, HttpServletResponse 
			response,String data,Cookie ticket) 
			throws IOException, ServletException {
		
		LogHelper.info("^^^^^^代理商系统^^^^^^^登录系统data:"+data);
		
		Map<String, Object> jsonStr = parseSourceJson(data);
		JSONObject json = new JSONObject();
		if(!jsonStr.containsKey("username") || !jsonStr.containsKey("password")){
			json.put("rspCode", "005");
			json.put("rspDesc", "用户名或密码不能为空!!!");
		}else{
			String password = jsonStr.get("password").toString();
			password = Utils.decrypt(password, this.secretKey);//判断是否是记住密码
			String keepLoginForWeek = "0";
			if(!Utils.isNullOrEmpty(jsonStr.get("keepLoginForWeek"))){
				keepLoginForWeek = jsonStr.get("keepLoginForWeek").toString();
			}
			/****/
			Cookie cookie = null;
			Map<String, Object> result = this.jdbcClient.queryAdmin(jsonStr.get("username").toString());
			if(result.isEmpty()){
				json.put("rspCode", "003");
				json.put("rspDesc", "用户不存在!!!");
			}else if(!password.equals(result.get("passWord"))){
				json.put("rspCode", "002");
				json.put("rspDesc", "用户名或者密码错误!!!");
			}else{
				cookie = createSsoCookie(result,ticket,keepLoginForWeek);
				json.put("rspCode", "000");
				json.put("rspDesc", "");
				LogHelper.info("^^^^^代理商系统^^^^^^^登录成功data:"+data);
			}
			/****/
			if(cookie != null){
				response.addCookie(cookie);
				response.addCookie(createUserCookie(result));
			}
		}
		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		out.close();
	}
	/**
	 * 
	 * @param request
	 * @param response
	 * @param data
	 * @param ticket
	 * @throws IOException 
	 */
	private void loginFromBusiness(HttpServletRequest request, HttpServletResponse 
			response,String data,Cookie ticket) throws IOException{
		
		data = URLDecoder.decode(data, "UTF-8");
		LogHelper.info("^^^^^^业务系统跳转到代理商系统^^^^^^^登录系统data:"+data);
		Map<String, Object> jsonStr = parseSourceJson(data);
		JSONObject json = new JSONObject();
		if(!jsonStr.containsKey("username") || !jsonStr.containsKey("agentId")){
			json.put("rspCode", "005");
			json.put("rspDesc", "用户名或商户ID不能为空!!!");
		}else{
			Map<String, Object> result = this.jdbcClient.
					queryForAgentId(jsonStr.get("agentId").toString());
			Cookie cookie = null;
			if(result.isEmpty()){
				json.put("rspCode", "003");
				json.put("rspDesc", "用户不存在,请创建用户!!!");
			}else{
				result.put("age_userName", "sys_"+jsonStr.get("username"));
				cookie = createSsoCookie(result,null,"0");
				json.put("rspCode", "000");
				json.put("rspDesc", "");
				LogHelper.info("^^^^^^业务系统跳转到代理商系统^^^^^^^登录成功data:"+data);
			}
			if(cookie != null){
				response.addCookie(cookie);
				response.addCookie(createUserCookie(result));
			}
		}
		String callback = request.getParameter("callbackparam");
		response.setContentType("text/json;charset=utf-8");
		PrintWriter out = response.getWriter();
		out.append(callback).append("(").append(json.toString()).append(")");
		out.close();
	}
	/**
	 * 
	 * @param request
	 * @return
	 */
	private String obtainLoginPageUrl(HttpServletRequest request){
		
		StringBuilder urlNew = new StringBuilder();
		urlNew.append(request.getScheme()).append("://");
		urlNew.append(request.getServerName()).append(":");
		urlNew.append(request.getServerPort()).append(request.getContextPath());
		urlNew.append("/agents/login.html");
		return urlNew.toString();
	}
	/**
	 * 创建用于存储用户名,密码等的cookie
	 * 所属分公司id
       所属分公司名称
       代理商id
       上级代理id
	 * @param username
	 * @param pass
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	 private Cookie createUserCookie(Map<String, Object> result) 
			throws UnsupportedEncodingException{
		
        JSONObject json = new JSONObject();
        json.put("userId", checkTextIsOrNotNull(result.get("userId")));
    	json.put("age_userName", checkTextIsOrNotNull(result.get("age_userName")));
    	String addPrefix = "prefix_"+DESUtils.encrypt(checkTextIsOrNotNull(result.get("passWord")), this.secretKey);
		json.put("passWord", addPrefix);
    	json.put("nickName", checkTextIsOrNotNull(result.get("nickName")));
    	json.put("phone", checkTextIsOrNotNull(result.get("phone")));
    	json.put("userState", checkTextIsOrNotNull(result.get("userState")));
    	json.put("level", checkTextIsOrNotNull(result.get("level")));//代理商级别
    	json.put("companyId", checkTextIsOrNotNull(result.get("companyId")));//所属分公司id
    	json.put("companyName", checkTextIsOrNotNull(result.get("companyName")));// 所属分公司名称
    	json.put("agentId", checkTextIsOrNotNull(result.get("agentId")));//代理商id
    	json.put("agentName", checkTextIsOrNotNull(result.get("agentName")));//代理商名称
    	json.put("superAgentId", checkTextIsOrNotNull(result.get("superAgentId")));//上级代理id
		int expiry = this.ticketTimeout*60;
		Cookie cookie = new Cookie(this.userCookieName, URLEncoder.encode(json.toString(), "UTF-8"));
		cookie.setSecure(this.secure);// 为true时用于https
		cookie.setMaxAge(expiry);
		cookie.setPath("/");
		return cookie;
	}
	 
	/**
	 * 
	 * @param request
	 * @return
	 */
	private Cookie getCookieFromRequest(HttpServletRequest request,String cookieName){
		
		Cookie ticket = null;
		Cookie[] cookies = request.getCookies();
		if(cookies != null){
			for(Cookie cookie : cookies) {
				if(cookie.getName().equals(cookieName)) {
					ticket = cookie;
					break;
				}
			}
		}
		return ticket;
	}
    /**
     * 
     * @param request
     * @param response
     * @throws IOException
     */
	private Cookie createSsoCookie(Map<String, Object> result,Cookie ticket,
			String keepLoginForWeek) throws IOException{
        
		/**username&userId&nick&password**/
		int expiry = this.ticketTimeout*60;
		if("1".equals(keepLoginForWeek)){
			expiry = 7*24*60*60;
		}
		if(ticket != null){
			String encodedTicket = ticket.getValue();
			String decodedTicket = DESUtils.decrypt(encodedTicket, this.secretKey);
			String ticketTime = this.redisClient.getTicket(decodedTicket);
			if(ticketTime != null){
				Map<String, Object> map = parseSourceJson(ticketTime);
				int expiryOne = checkExpiryTime(ticketTime);
				if(expiryOne > 0){
					this.redisClient.updateTicket(decodedTicket, obtainNewExpiryTime(map,expiry/60,false));
					return null;
				}
			}
		}
		String ticketKey = UUID.randomUUID().toString().replace("-", "");
		String encodedticketKey = DESUtils.encrypt(ticketKey, this.secretKey);
		this.redisClient.insertTicket(ticketKey, obtainNewExpiryTime(result,expiry/60,true));
		/****/
		Cookie cookie = new Cookie(this.cookieName, encodedticketKey);
		cookie.setSecure(this.secure);// 为true时用于https
		cookie.setMaxAge(expiry);
		cookie.setPath("/");
		return cookie;
	}
	/**
	 * 
	 * @param userName
	 * @return
	 */
    private String obtainNewExpiryTime(Map<String, Object> result,int ticketTimeout,boolean createTicket){
    	
    	LogHelper.info("result:"+result+"\ncreateTicket:"+createTicket);
    	JSONObject json = new JSONObject();
    	json.put("userId", checkTextIsOrNotNull(result.get("userId")));
    	json.put("age_userName", checkTextIsOrNotNull(result.get("age_userName")));
    	if(createTicket){
    		String addPrefix = "prefix_"+DESUtils.encrypt(checkTextIsOrNotNull(result.get("passWord")), this.secretKey);
    		json.put("passWord", addPrefix);
    	}else{
    		json.put("passWord", checkTextIsOrNotNull(result.get("passWord")));
    	}
    	json.put("level", checkTextIsOrNotNull(result.get("level")));
    	json.put("nickName", checkTextIsOrNotNull(result.get("nickName")));
    	json.put("phone", checkTextIsOrNotNull(result.get("phone")));
    	json.put("userState", checkTextIsOrNotNull(result.get("userState")));
    	json.put("companyId", checkTextIsOrNotNull(result.get("companyId")));
    	json.put("companyName", checkTextIsOrNotNull(result.get("companyName")));
    	json.put("agentId", checkTextIsOrNotNull(result.get("agentId")));
    	json.put("agentName", checkTextIsOrNotNull(result.get("agentName")));
    	json.put("superAgentId", checkTextIsOrNotNull(result.get("superAgentId")));
    	json.put("ticketTimeout", getRecoverTime(ticketTimeout));
		return json.toString();
    }
    /**
     * 
     * @param source
     * @return
     */
    private String checkTextIsOrNotNull(Object source){
    	if(!Utils.isNullOrEmpty(source)){
    		return source.toString();
    	}
    	return "";
    }
    
	/**
	 * 
	 * @param ticketTime
	 * @return
	 * @throws IOException 
	 */
	private int checkExpiryTime(String ticketTime) throws IOException{
		
		Map<String, Object> map = parseSourceJson(ticketTime);
		long expireTimeOne = Long.parseLong(map.get("ticketTimeout").toString());
		long expiry = (expireTimeOne - System.currentTimeMillis())/1000;
		return new Long(expiry).intValue();
	}
	
	@SuppressWarnings("unchecked")
	private Map<String,Object> parseSourceJson(String source) throws IOException{
		
		JSONReader jsonReader = new JSONReader();
		Object obj = jsonReader.read(source);
		if(obj instanceof Map<?, ?>){
			return (Map<String, Object>)obj;
		}
		return null;
	}
	/**
	 * 
	 * @param time
	 * @return
	 */
	private long getRecoverTime(int time){
		
		Timestamp createTime = new Timestamp(System.currentTimeMillis());
		Calendar cal = Calendar.getInstance();
		cal.setTime(createTime);
		cal.add(Calendar.MINUTE, time);
		Timestamp recoverTime = new Timestamp(cal.getTimeInMillis());
		return recoverTime.getTime();
	}
	
	@Override
	public void destroy() {
	}
}
