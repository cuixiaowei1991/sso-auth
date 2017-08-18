package com.cn.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 跨域支持过滤器，在HTTP响应中增加一些头信息
 * @author songzhili
 * 2016年7月1日上午9:04:29
 */
public class CrossDomainFilter implements Filter {
	
	    public void init(FilterConfig filterConfig) throws ServletException {
	    }

	    /**
	     * 跨域支持配置
	     * @param servletRequest
	     * @param servletResponse
	     * @param filterChain
	     * @throws IOException
	     * @throws ServletException
	     */
	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest)servletRequest;
		HttpServletResponse response = (HttpServletResponse)servletResponse;
		request.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods",
				"GET,POST,PUT,DELETE,OPTIONS");
		response.setHeader("Access-Control-Allow-Headers",
				"Content-Type,TestKey,x-requested-with");
		filterChain.doFilter(request, response);
	}

	public void destroy() {

	}

}
