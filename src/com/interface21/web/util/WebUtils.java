package com.interface21.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Miscellaneous utilities for web applications.
 * Also used by various framework classes.
 * @author Rod Johnson, Juergen Hoeller
 */
public abstract class WebUtils {

	/** HTTP header value */
	public static final String HEADER_IFMODSINCE = "If-Modified-Since";

	/** HTTP header value */
	public static final String HEADER_LASTMOD = "Last-Modified";

	public static final String WEB_APP_ROOT_KEY_PARAM = "webAppRootKey";

	/**
	 * Set a system property to the web application root directory.
	 * The key of the system property can be defined with the
	 * "webAppRootKey" init parameter at the servlet context level.
	 *
	 * <p>Can be used for toolkits that support substition with
	 * system properties (i.e. System.getProperty values),
	 * like Log4J's ${key} syntax within log file locations.
	 *
	 * @param servletContext the servlet context of the web application
	 * @see #WEB_APP_ROOT_KEY_PARAM
	 * @see WebAppRootListener
	 */
	public static void setWebAppRootSystemProperty(ServletContext servletContext) {
		String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
		String key = (param != null ? param : "webapp.root");
		String oldValue = System.getProperty(key);
		if (oldValue != null) {
			servletContext.log("WARNING: Web app root system property already set: "  + key + " = " + oldValue);
			servletContext.log("WARNING: Choose unique webAppRootKey values in your web.xml files!");
		} else {
			String root = servletContext.getRealPath("/");
			System.setProperty(key, root);
			servletContext.log("Set web app root system property: " + key + " = " + root);
		}
	}

	/**
	 * Given a String which may be either a URL, an absolute file location, or a location
	 * within the WAR. Opens an input stream allowing access to its contents.
	 * Note: Callers are responsible for closing the input stream.
	 * @param location String which may be either a URL (e.g. http://somecompany.com/foo.xml),
	 * an absolute file location, or a location within the WAR (e.g. /WEB-INF/data/file.dat)
	 * @param servletContext the application's servlet context. We'll need this if we
	 * need to load from within the WAR
	 * @throws IOException if we can't open the resource
	 * @return an InputStream for the resources contents.
	 */
	public static InputStream getResourceInputStream(String location, ServletContext servletContext) throws IOException {
		try {
			// try URL
			URL url = new URL(location);
			return url.openStream();

		} catch (MalformedURLException ex) {
			// no URL -> file location
			File file = new File(location);
			if (!file.isAbsolute()) {
				// Load from within WAR
				if (!location.startsWith("/"))
					location = "/" + location;
				InputStream is = servletContext.getResourceAsStream(location);
				if (is == null)
					throw new IOException("Can't open " + location);
				return is;
			} else {
				// Remote loading
				return new FileInputStream(location);
			}
		}
	}

	/**
	 * Retrieve the first cookie with the given name.
	 * @return the first cookie with the given name, or null if none is found
	 * Note that multiple cookies can have the same name but different paths or domains
	 * @param name cookie name
	 */
	public static Cookie getCookie(HttpServletRequest request, String name) {
		Cookie cookies[] = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				if (name.equals(cookies[i].getName()))
					return cookies[i];
			}
		}
		return null;
	}

	/**
	 * Return the URL of the root of the current application.
	 * @param request current request
	 */
	public static String getUrlToApplication(HttpServletRequest request) {
		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";
	}

	/**
	 * Return the path within the web application for the given request.
	 * @param request current request
	 * @return the path within the web application
	 */
	public static String getPathWithinApplication(HttpServletRequest request) {
		return request.getRequestURI().substring(request.getContextPath().length());
	}

	/**
	 * Return the path within the servlet mapping for the given request,
	 * i.e. the part of the request's URL beyond the part that called the servlet,
	 * or "" if the whole URL has been used to identify the servlet.
	 * <p>E.g.: servlet mapping = "/test/*"; request URI = "/test/a" -> "/a".
	 * <p>E.g.: servlet mapping = "/test"; request URI = "/test" -> "".
	 * <p>E.g.: servlet mapping = "/*.test"; request URI = "/a.test" -> "".
	 * @param request current request
	 * @return the path within the servlet mapping, or ""
	 */
	public static String getPathWithinServletMapping(HttpServletRequest request) {
		int servletIndex = request.getRequestURI().indexOf(request.getServletPath());
		return request.getRequestURI().substring(servletIndex + request.getServletPath().length());
	}

	/**
	 * Given a servlet path string, determine the directory within the WAR
	 * this belongs to, ending with a /. For example, /cat/dog/test.html would be
	 * returned as /cat/dog/. /test.html would be returned as /
	 */
	public static String getDirectoryForServletPath(String servletPath) {
		// Arg will be of form /dog/cat.jsp. We want to see /dog/
		if (servletPath == null || servletPath.indexOf("/") == -1)
			return "/";
		String left = servletPath.substring(0, servletPath.lastIndexOf("/") + 1);
		return left;
	}

	/**
	 * Convenience method to return a map from un-prefixed property names
	 * to values. E.g. with a prefix of price, price_1, price_2 produce
	 * a properties object with mappings for 1, 2 to the same values.
	 * @param request HTTP request in which to look for parameters
	 * @param base beginning of parameter name. If this is null or the empty string,
	 * all parameters will match
	 * @return properties mapping request parameters <b>without the prefix</b>
	 */
	public static Properties getParametersStartingWith(ServletRequest request, String base) {
		Enumeration enum = request.getParameterNames();
		Properties props = new Properties();
		if (base == null)
			base = "";
		while (enum != null && enum.hasMoreElements()) {
			String paramName = (String) enum.nextElement();
			if (base == null || "".equals(base) || paramName.startsWith(base)) {
				String val = request.getParameter(paramName);
				String unprefixed = paramName.substring(base.length());
				props.setProperty(unprefixed, val);
			}
		}
		return props;
	}

}
