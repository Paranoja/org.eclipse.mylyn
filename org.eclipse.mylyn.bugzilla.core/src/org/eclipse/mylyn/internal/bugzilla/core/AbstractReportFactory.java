/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.bugzilla.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import javax.security.auth.login.LoginException;

import org.eclipse.mylar.internal.tasks.core.UrlConnectionUtil;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Rob Elves
 */
public class AbstractReportFactory {

	private static final String ENCODING_GZIP = "gzip";

	private static final int COM_TIME_OUT = 30000;

	private static final String CONTENT_TYPE_TEXT_HTML = "text/html";

	private static final String CONTENT_TYPE_APP_RDF_XML = "application/rdf+xml";

	private static final String CONTENT_TYPE_APP_XML = "application/xml";

	private static final String CONTENT_TYPE_TEXT_XML = "text/xml";

	public static final int RETURN_ALL_HITS = -1;

	protected void collectResults(URL url, Proxy proxySettings, String characterEncoding,
			DefaultHandler contentHandler, boolean clean) throws IOException, LoginException, KeyManagementException,
			NoSuchAlgorithmException, BugzillaException {
		URLConnection cntx = UrlConnectionUtil.getUrlConnection(url, proxySettings, false);
		if (cntx == null || !(cntx instanceof HttpURLConnection)) {
			throw new IOException("Could not form URLConnection.");
		}

		HttpURLConnection connection = (HttpURLConnection) cntx;
		try {
			connection.setConnectTimeout(COM_TIME_OUT);
			connection.setReadTimeout(COM_TIME_OUT);
			connection.addRequestProperty("Accept-Encoding", ENCODING_GZIP);

			connection.connect();
			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				String msg;
				if (responseCode == -1 || responseCode == HttpURLConnection.HTTP_FORBIDDEN)
					msg = "Repository does not seem to be a valid Bugzilla server: " + url.toExternalForm();
				else
					msg = "HTTP Error " + responseCode + " (" + connection.getResponseMessage()
							+ ") while querying Bugzilla server: " + url.toExternalForm();

				throw new IOException(msg);
			}

			BufferedReader in = null;

			String contentEncoding = connection.getContentEncoding();
			boolean gzipped = contentEncoding != null && ENCODING_GZIP.equals(contentEncoding);
			if (characterEncoding != null) {
				if (gzipped) {
					in = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream()),
							characterEncoding));
				} else {
					in = new BufferedReader(new InputStreamReader(connection.getInputStream(), characterEncoding));
				}
			} else {
				if (gzipped) {
					in = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream())));
				} else {
					in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				}
			}

			if (clean) {
				StringBuffer result = XmlCleaner.clean(in);
				StringReader strReader = new StringReader(result.toString());
				in = new BufferedReader(strReader);
			}

			if (connection.getContentType().contains(CONTENT_TYPE_APP_RDF_XML)
					|| connection.getContentType().contains(CONTENT_TYPE_APP_XML)
					|| connection.getContentType().contains(CONTENT_TYPE_TEXT_XML)) {

				try {
					final XMLReader reader = XMLReaderFactory.createXMLReader();
					reader.setContentHandler(contentHandler);
					reader.setErrorHandler(new ErrorHandler() {

						public void error(SAXParseException exception) throws SAXException {
							throw exception;
						}

						public void fatalError(SAXParseException exception) throws SAXException {
							throw exception;
						}

						public void warning(SAXParseException exception) throws SAXException {
							throw exception;
						}
					});
					reader.parse(new InputSource(in));
				} catch (SAXException e) {
					if (e.getMessage().equals(IBugzillaConstants.ERROR_INVALID_USERNAME_OR_PASSWORD)) {
						throw new LoginException(e.getMessage());
					} else {
						throw new IOException(e.getMessage());
					}
				}
			} else if (connection.getContentType().contains(CONTENT_TYPE_TEXT_HTML)) {
//				try {
					BugzillaServerFacade.parseHtmlError(in);
//				} catch (BugzillaException e) {
//					throw new IOException(e.getMessage());
//				}
			} else {
				throw new IOException("Unrecognized content type: " + connection.getContentType());
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
