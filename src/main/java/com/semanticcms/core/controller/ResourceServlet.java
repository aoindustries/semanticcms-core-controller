/*
 * semanticcms-core-controller - Serves SemanticCMS content from a Servlet environment.
 * Copyright (C) 2018  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-core-controller.
 *
 * semanticcms-core-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-core-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-core-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.core.controller;

import com.aoindustries.io.IoUtils;
import com.semanticcms.core.resources.ResourceConnection;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TODO: This has a lot of redundancy with DefaultServlet.  Is there a way to leverage DefaultServlet
 * for this purpose?
 */
@WebServlet(name = ResourceServlet.NAME)
public class ResourceServlet extends HttpServlet {

	protected static final String NAME = "com.semanticcms.core.controller.ResourceServlet";

	protected static final String RESOURCE_CONN_REQUEST_PARAMETER = ResourceServlet.class.getName() + ".resourceConn";

	private static final long serialVersionUID = 1L;

	public static void dispatch(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		ResourceConnection resourceConn
	) throws IOException, ServletException {
		RequestDispatcher dispatcher = servletContext.getNamedDispatcher(ResourceServlet.NAME);
		if(dispatcher == null) throw new ServletException("RequestDispatcher not found: " + ResourceServlet.NAME);
		Object oldResourceConn = request.getAttribute(ResourceServlet.RESOURCE_CONN_REQUEST_PARAMETER);
		try {
			request.setAttribute(ResourceServlet.RESOURCE_CONN_REQUEST_PARAMETER, resourceConn);
			dispatcher.forward(request, response);
		} finally {
			request.setAttribute(ResourceServlet.RESOURCE_CONN_REQUEST_PARAMETER, oldResourceConn);
		}
	}

	protected static ResourceConnection getResourceConn(ServletRequest request) throws ServletException {
		ResourceConnection resourceConn = (ResourceConnection)request.getAttribute(RESOURCE_CONN_REQUEST_PARAMETER);
		if(resourceConn == null) throw new ServletException("Request parameter not set: " + RESOURCE_CONN_REQUEST_PARAMETER);
		return resourceConn;
	}

	@Override
	protected long getLastModified(HttpServletRequest request) {
		try {
			ResourceConnection resourceConn = getResourceConn(request);
			long lastModified = resourceConn.getLastModified();
			return lastModified == 0 ? -1 : lastModified;
		} catch(IOException e) {
			log(null, e);
			return -1;
		} catch(ServletException e) {
			log(null, e);
			return -1;
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ResourceConnection resourceConn = getResourceConn(request);
		// TODO: checkIfHeaders
		// Get content type
		String contentType = "text/plain"; // TODO: Get from resourceConnection
		response.setContentType(contentType);
		// TODO: eTag
		// TODO: Support pre-compression?
		// TODO: Ranges
		// TODO: Support sendFile: https://tomcat.apache.org/tomcat-9.0-doc/aio.html
		// TODO: Support getContent as byte[] on ResourceConnection?
		// Length
		long length = resourceConn.getLength();
		if(length != -1) {
			if(length < 0) throw new AssertionError();
			if(length <= Integer.MAX_VALUE) {
				response.setContentLength((int)length);
			} else {
				// TODO: Servlet 3.1: response.setContentLengthLong(length);
			}
		}
		ServletOutputStream out = response.getOutputStream();
		InputStream in = resourceConn.getInputStream();
		try {
			long copied = IoUtils.copy(in, out);
			if(length != -1 && copied != length) throw new ServletException("Wrong number of bytes copied for " + resourceConn + ": length = " + length + ", copied = " + copied);
		} finally {
			in.close();
		}
	}
}
