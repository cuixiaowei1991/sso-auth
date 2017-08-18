package com.cn.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 网络工具类。
 * 
 * @author carver.gu
 * @since 1.0, Sep 12, 2009
 */
@SuppressWarnings("unused")
public abstract class WebUtils {

	private static final String DEFAULT_CHARSET = "UTF-8";
	private static final String METHOD_POST = "POST";
	private static final String METHOD_GET = "GET";

	private static class DefaultTrustManager implements X509TrustManager {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}
	}

	private WebUtils() {
	}

	/**
	 * 执行HTTP POST请求。
	 * 
	 * @param url 请求地址
	 * @param params 请求参数
	 * @return 响应字符串
	 * @throws IOException
	 */
	public static String doPost(String url, Map<String, Object> params,int connectTimeout,int readTimeout) throws IOException {
		return doPost(url, params, DEFAULT_CHARSET,connectTimeout,readTimeout);
	}

	/**
	 * 执行HTTP POST请求。
	 * 
	 * @param url 请求地址
	 * @param params 请求参数
	 * @param charset 字符集，如UTF-8, GBK, GB2312
	 * @return 响应字符串
	 * @throws IOException
	 */
	public static String doPost(String url, Map<String, Object> params, String charset,int connectTimeout,int readTimeout)
			throws IOException {
		String ctype = "application/x-www-form-urlencoded;charset=" + charset;
		String query = buildQuery(params, charset);
		byte[] content = {};
		if(query!=null){
			content = query.getBytes(charset);
		}
		return doPost(url, ctype, content, connectTimeout, readTimeout);
	}

	/**
	 * 执行HTTP POST请求。
	 * 
	 * @param url 请求地址
	 * @param ctype 请求类型
	 * @param content 请求字节数组
	 * @return 响应字符串
	 * @throws IOException
	 */
	public static String doPost(String url, String ctype, byte[] content,int connectTimeout,int readTimeout) throws IOException {
		HttpURLConnection conn = null;
		OutputStream out = null;
		String responseData = null;
		try {
			try{
				conn = getConnection(new URL(url), METHOD_POST, ctype);	
				conn.setConnectTimeout(connectTimeout);
				conn.setReadTimeout(readTimeout);
			}catch(IOException e){
				Map<String, String> map = getParamsFromUrl(url);
				throw e;
			}
			try{
				out = conn.getOutputStream();
				out.write(content);
				responseData = getResponseAsString(conn);
			}catch(IOException e){
				Map<String, String> map = getParamsFromUrl(url);
				throw e;
			}
			
		}finally {
			if (out != null) {
				out.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}
		return responseData;
	}
	/**
	 * 
	 * @param url
	 * @param method
	 * @param ctype
	 * @return
	 * @throws IOException
	 */
	private static HttpURLConnection getConnection(URL url, String method, String ctype) throws IOException {
		HttpURLConnection conn = null;
		if ("https".equals(url.getProtocol())) {
			SSLContext ctx = null;
			try {
				ctx = SSLContext.getInstance("TLS");
				ctx.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
			} catch (Exception e) {
				throw new IOException(e);
			}
			HttpsURLConnection connHttps = (HttpsURLConnection) url.openConnection();
			connHttps.setSSLSocketFactory(ctx.getSocketFactory());
			connHttps.setHostnameVerifier(new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;// 默认都认证通过
				}
			});
			conn = connHttps;
		} else {
			conn = (HttpURLConnection) url.openConnection();
		}
		conn.setRequestMethod(method);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Accept", "*");
		conn.setRequestProperty("User-Agent", "top-sdk-java");
		conn.setRequestProperty("Content-Type", ctype);
		return conn;
	}
	/**
	 * 
	 * @param url
	 * @return
	 */
	private static Map<String, String> getParamsFromUrl(String url) {
		Map<String,String> map=null;
		if(url!=null&&url.indexOf('?')!=-1){
			map=splitUrlQuery(url.substring(url.indexOf('?')+1));
		}
		if(map==null){
			map=new HashMap<String,String>();
		}
		return map;
	}
	/**
	 * 
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	private static String getResponseAsString(HttpURLConnection conn) throws IOException {
		
		String charset = getResponseCharset(conn.getContentType());
		InputStream es = conn.getErrorStream();
		if (es == null) {
			return getStreamAsString(conn.getInputStream(), charset);
		} else {
			String msg = getStreamAsString(es, charset);
			if (Utils.isEmpty(msg)) {
				throw new IOException(conn.getResponseCode() + ":" + conn.getResponseMessage());
			} else {
				throw new IOException(msg);
			}
		}
	}
    /**
     * 
     * @param stream
     * @param charset
     * @return
     * @throws IOException
     */
	private static String getStreamAsString(InputStream stream, String charset) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset));
			StringWriter writer = new StringWriter();
			char[] chars = new char[256];
			int count = 0;
			while ((count = reader.read(chars)) > 0) {
				writer.write(chars, 0, count);
			}
			return writer.toString();
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}
   /**
    * 
    * @param ctype
    * @return
    */
	private static String getResponseCharset(String ctype) {
		
		String charset = DEFAULT_CHARSET;
		if (!Utils.isEmpty(ctype)) {
			String[] params = ctype.split(";");
			for (String param : params) {
				param = param.trim();
				if (param.startsWith("charset")) {
					String[] pair = param.split("=", 2);
					if (pair.length == 2) {
						if (!Utils.isEmpty(pair[1])) {
							charset = pair[1].trim();
						}
					}
					break;
				}
			}
		}
		return charset;
	}
	/**
	 * 
	 * @param query
	 * @return
	 */
	private static Map<String, String> splitUrlQuery(String query) {
		Map<String, String> result = new HashMap<String, String>();
		String[] pairs = query.split("&");
		if (pairs != null && pairs.length > 0) {
			for (String pair : pairs) {
				String[] param = pair.split("=", 2);
				if (param != null && param.length == 2) {
					result.put(param[0], param[1]);
				}
			}
		}
		return result;
	}
	/**
	 * 
	 * @param params
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	private static String buildQuery(Map<String, Object> params, String charset) throws IOException {
		
		if (params == null || params.isEmpty()) {
			return null;
		}
		StringBuilder query = new StringBuilder();
		Set<Entry<String, Object>> entries = params.entrySet();
		boolean hasParam = false;
		for (Entry<String, Object> entry : entries) {
			String name = entry.getKey();
			String value = entry.getValue().toString();
			// 忽略参数名或参数值为空的参数
			if (!Utils.isEmpty(name)
					&& !Utils.isEmpty(value)) {
				if (hasParam) {
					query.append("&");
				} else {
					hasParam = true;
				}
				query.append(name).append("=").append(URLEncoder.encode(value, charset));
			}
		}
		return query.toString();
	}

}
