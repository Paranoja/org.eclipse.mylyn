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
package org.eclipse.mylar.internal.tasklist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylar.core.util.MylarStatusHandler;
import org.eclipse.mylar.tasklist.ITask;
import org.eclipse.mylar.tasklist.ITaskCategory;
import org.eclipse.mylar.tasklist.ITaskHandler;
import org.eclipse.mylar.tasklist.ITaskListExternalizer;
import org.eclipse.mylar.tasklist.IRepositoryQuery;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Mik Kersten
 * @author Ken Sueda
 */
public class TaskListWriter {

	public static final String ATTRIBUTE_VERSION = "Version";

	public static final String ELEMENT_TASK_LIST = "TaskList";

	private static final String VALUE_VERSION = "1.0.1";

	private static final String VALUE_VERSION_1_0_0 = "1.0.0";

	private static final String FILE_SUFFIX_SAVE = "save.xml";

	private List<ITaskListExternalizer> externalizers;

	private DelegatingLocalTaskExternalizer delagatingExternalizer = new DelegatingLocalTaskExternalizer();

	private String readVersion = "";

	private boolean hasCaughtException = false;

	public void setDelegateExternalizers(List<ITaskListExternalizer> externalizers) {
		this.externalizers = externalizers;
		this.delagatingExternalizer.setDelegateExternalizers(externalizers);
	}

	public void writeTaskList(TaskList tlist, File outFile) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document doc = null;

		try {
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
		} catch (ParserConfigurationException e) {
			MylarStatusHandler.log(e, "could not create document");
			e.printStackTrace();
		}

		Element root = doc.createElement(ELEMENT_TASK_LIST);
		root.setAttribute(ATTRIBUTE_VERSION, VALUE_VERSION);

		for (ITaskListExternalizer externalizer : externalizers) {
			externalizer.createRegistry(doc, root);
		}

		for (ITaskCategory category : tlist.getCategories()) {
			Element element = null;
			for (ITaskListExternalizer externalizer : externalizers) {
				if (externalizer.canCreateElementFor(category))
					element = externalizer.createCategoryElement(category, doc, root);
			}
			if (element == null && delagatingExternalizer.canCreateElementFor(category)) {
				delagatingExternalizer.createCategoryElement(category, doc, root);
			} else if (element == null) {
				MylarStatusHandler.log("Did not externalize: " + category, this);
			}
		}

		for (IRepositoryQuery query : tlist.getQueries()) {
			Element element = null;
			for (ITaskListExternalizer externalizer : externalizers) {
				if (externalizer.canCreateElementFor(query))
					element = externalizer.createQueryElement(query, doc, root);
			}
			if (element == null && delagatingExternalizer.canCreateElementFor(query)) {
				delagatingExternalizer.createQueryElement(query, doc, root);
			} else if (element == null) {
				MylarStatusHandler.log("Did not externalize: " + query, this);
			}
		}

		for (ITask task : tlist.getRootTasks()) {
			try {
				Element element = null;
				for (ITaskListExternalizer externalizer : externalizers) {
					if (externalizer.canCreateElementFor(task))
						element = externalizer.createTaskElement(task, doc, root);
				}
				if (element == null && delagatingExternalizer.canCreateElementFor(task)) {
					delagatingExternalizer.createTaskElement(task, doc, root);
				} else if (element == null) {
					MylarStatusHandler.log("Did not externalize: " + task, this);
				}
			} catch (Exception e) {
				MylarStatusHandler.log(e, e.getMessage());
			}
		}
		doc.appendChild(root);
		writeDOMtoFile(doc, outFile);
		return;
	}

	/**
	 * Writes an XML file from a DOM.
	 * 
	 * doc - the document to write file - the file to be written to
	 */
	private void writeDOMtoFile(Document doc, File file) {
		try {
			// A file output stream is an output stream for writing data to a
			// File
			//
			OutputStream outputStream = new FileOutputStream(file);
			writeDOMtoStream(doc, outputStream);
			outputStream.flush();
			outputStream.close();
		} catch (Exception fnfe) {
			MylarStatusHandler.log(fnfe, "TaskList could not be found");
		}
	}

	/**
	 * Writes the provided XML document out to the specified output stream.
	 * 
	 * doc - the document to be written outputStream - the stream to which the
	 * document is to be written
	 */
	private void writeDOMtoStream(Document doc, OutputStream outputStream) {
		// Prepare the DOM document for writing
		// DOMSource - Acts as a holder for a transformation Source tree in the
		// form of a Document Object Model (DOM) tree
		//
		Source source = new DOMSource(doc);

		// StreamResult - Acts as an holder for a XML transformation result
		// Prepare the output stream
		//
		Result result = new StreamResult(outputStream);

		// An instance of this class can be obtained with the
		// TransformerFactory.newTransformer method. This instance may
		// then be used to process XML from a variety of sources and write
		// the transformation output to a variety of sinks
		//

		Transformer xformer = null;
		try {
			xformer = TransformerFactory.newInstance().newTransformer();
			// Transform the XML Source to a Result
			//
			xformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * TODO: fix this old mess
	 */
	public void readTaskList(TaskList tlist, File inFile) {
		// initExtensions();
		// MylarTaskListPlugin.getDefault().restoreTaskHandlerState();
		hasCaughtException = false;
		try {
			// parse file
			//
			if (!inFile.exists())
				return;
			Document doc = openAsDOM(inFile);
			if (doc == null) {
				handleException(inFile, null, new TaskListExternalizerException("TaskList was not well formed XML"));
				return;
			}
			// read root node to get version number
			//
			Element root = doc.getDocumentElement();
			readVersion = root.getAttribute(ATTRIBUTE_VERSION);

			if (readVersion.equals(VALUE_VERSION_1_0_0)) {
				MylarStatusHandler.log("version: " + readVersion + " not supported", this);
				// NodeList list = root.getChildNodes();
				// for (int i = 0; i < list.getLength(); i++) {
				// Node child = list.item(i);
				// readTasksToNewFormat(child, tlist);
				// //tlist.addRootTask(readTaskAndSubTasks(child, null, tlist));
				// }
			} else {
				NodeList list = root.getChildNodes();
				for (int i = 0; i < list.getLength(); i++) {
					Node child = list.item(i);
					boolean wasRead = false;
					try {
						if (child.getNodeName().endsWith(DelegatingLocalTaskExternalizer.TAG_CATEGORY)) {
							for (ITaskListExternalizer externalizer : externalizers) {
								if (externalizer.canReadCategory(child)) {
									externalizer.readCategory(child, tlist);
									wasRead = true;
									break;
								}
							}
							if (!wasRead && delagatingExternalizer.canReadCategory(child)) {
								delagatingExternalizer.readCategory(child, tlist);
							} else {
								// MylarPlugin.log("Did not read: " +
								// child.getNodeName(), this);
							}
						} else if (child.getNodeName().endsWith(DelegatingLocalTaskExternalizer.TAG_QUERY)) {
							for (ITaskListExternalizer externalizer : externalizers) {
								if (externalizer.canReadQuery(child)) {
									externalizer.readQuery(child, tlist);
									wasRead = true;
									break;
								}
							}
							if (!wasRead && delagatingExternalizer.canReadCategory(child)) {
								delagatingExternalizer.readQuery(child, tlist);
							} else {
								// MylarPlugin.log("Did not read: " +
								// child.getNodeName(), this);
							}
						} else {
							for (ITaskListExternalizer externalizer : externalizers) {
								if (externalizer.canReadTask(child)) {
									// TODO add the tasks properly
									ITask newTask = externalizer.readTask(child, tlist, null, null);
									ITaskHandler taskHandler = MylarTaskListPlugin.getDefault().getHandlerForElement(
											newTask);
									if (taskHandler != null) {
										newTask = taskHandler.addTaskToRegistry(newTask);
									}
									tlist.addRootTask(newTask);

									wasRead = true;
									break;
								}
							}
							if (!wasRead && delagatingExternalizer.canReadTask(child)) {
								tlist.addRootTask(delagatingExternalizer.readTask(child, tlist, null, null));
							} else {
								// MylarPlugin.log("Did not read: " +
								// child.getNodeName(), this);
							}
						}
					} catch (Exception e) {
						handleException(inFile, child, e);
					}
				}
			}
		} catch (Exception e) {
			handleException(inFile, null, e);
		}
		if (hasCaughtException) {
			// if exception was caught, write out the new task file, so that it
			// doesn't happen again.
			// this is OK, since the original (corrupt) tasklist is saved.
			// TODO: The problem with this is that if the orignal tasklist has
			// tasks and bug reports, but a
			// task is corrupted, the new tasklist that is written will not
			// include the bug reports (since the
			// bugzilla externalizer is not loaded. So there is a potentila that
			// we can lose bug reports.
			writeTaskList(tlist, inFile);
		}
		// MylarTaskListPlugin.getDefault().restoreTaskHandlerState();
	}

	/**
	 * Opens the specified XML file and parses it into a DOM Document.
	 * 
	 * Filename - the name of the file to open Return - the Document built from
	 * the XML file Throws - XMLException if the file cannot be parsed as XML -
	 * IOException if the file cannot be opened
	 */
	private Document openAsDOM(File inputFile) throws IOException {

		// A factory API that enables applications to obtain a parser
		// that produces DOM object trees from XML documents
		//
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		// Using DocumentBuilder, obtain a Document from XML file.
		//
		DocumentBuilder builder = null;
		Document document = null;
		try {
			// create new instance of DocumentBuilder
			//
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			inputFile.renameTo(new File(inputFile.getName() + FILE_SUFFIX_SAVE));
			MylarStatusHandler.log(pce, "Failed to load XML file");
		}
		try {
			// Parse the content of the given file as an XML document
			// and return a new DOM Document object. Also throws IOException
			document = builder.parse(inputFile);
		} catch (SAXException se) {
			File backup = new File(MylarTaskListPlugin.getDefault().getTaskListSaveManager().getBackupFilePath());
			String message = "Restoring the tasklist failed.  Would you like to attempt to restore from the backup?\n\nTasklist XML File location: "
					+ inputFile.getAbsolutePath()
					+ "\n\nBackup tasklist XML file location: "
					+ backup.getAbsolutePath();
			if (backup.exists() && MessageDialog.openQuestion(null, "Restore From Backup", message)) {
				try {
					document = builder.parse(backup);
					MylarTaskListPlugin.getDefault().getTaskListSaveManager().reverseBackup();
				} catch (SAXException s) {
					inputFile.renameTo(new File(inputFile.getName() + FILE_SUFFIX_SAVE));
					MylarStatusHandler.log(s, "Failed to recover from backup restore");
				}
			}
		}
		return document;
	}

	private void handleException(File inFile, Node child, Exception e) {
		hasCaughtException = true;
		String name = inFile.getAbsolutePath();
		name = name.substring(0, name.lastIndexOf('.')) + "-save1.xml";
		File save = new File(name);
		int i = 2;
		while (save.exists()) {
			name = name.substring(0, name.lastIndexOf('.') - 1) + i + ".xml";
			save = new File(name);
			i++;
		}
		if (!copy(inFile, save)) {
			inFile.renameTo(new File(name));
		}
		if (child == null) {
			MylarStatusHandler.log(e, "Could not read task list");
		} else {
			MylarStatusHandler.log(e, "Tasks may have been lost from " + child.getNodeName());
		}
	}

	private boolean copy(File src, File dst) {
		try {
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dst);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			return true;
		} catch (IOException ioe) {
			return false;
		}
	}

	/*
	 * 
	 * 
	 */
	private Document openAsDOM(String input) throws IOException {

		// A factory API that enables applications to obtain a parser
		// that produces DOM object trees from XML documents
		//
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		// Using DocumentBuilder, obtain a Document from XML file.
		//
		DocumentBuilder builder = null;
		Document document = null;
		try {
			// create new instance of DocumentBuilder
			//
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			MylarStatusHandler.log(pce, "Failed to load XML file");
		}
		try {
			// Parse the content of the given file as an XML document
			// and return a new DOM Document object. Also throws IOException
			StringReader s = new StringReader(input);
			InputSource in = new InputSource(s);
			document = builder.parse(in);
		} catch (SAXException se) {
			MylarStatusHandler.log(se, "Failed to parse XML file");
		}
		return document;
	}

	public void readTaskList(TaskList tlist, String input) {
		// initExtensions();
		try {

			Document doc = openAsDOM(input);
			if (doc == null) {
				return;
			}
			// read root node to get version number
			//
			Element root = doc.getDocumentElement();
			readVersion = root.getAttribute(ATTRIBUTE_VERSION);

			if (readVersion.equals(VALUE_VERSION_1_0_0)) {
				MylarStatusHandler.log("version: " + readVersion + " not supported", this);
				// NodeList list = root.getChildNodes();
				// for (int i = 0; i < list.getLength(); i++) {
				// Node child = list.item(i);
				// readTasksToNewFormat(child, tlist);
				// //tlist.addRootTask(readTaskAndSubTasks(child, null, tlist));
				// }
			} else {
				NodeList list = root.getChildNodes();
				for (int i = 0; i < list.getLength(); i++) {
					Node child = list.item(i);
					boolean wasRead = false;
					try {
						if (child.getNodeName().endsWith(DelegatingLocalTaskExternalizer.TAG_CATEGORY)) {
							for (ITaskListExternalizer externalizer : externalizers) {
								if (externalizer.canReadCategory(child)) {
									externalizer.readCategory(child, tlist);
									wasRead = true;
									break;
								}
							}
							if (!wasRead && delagatingExternalizer.canReadCategory(child)) {
								delagatingExternalizer.readCategory(child, tlist);
							} else {
								// MylarPlugin.log("Did not read: " +
								// child.getNodeName(), this);
							}
						} else {
							for (ITaskListExternalizer externalizer : externalizers) {
								if (externalizer.canReadTask(child)) {
									// TODO add the tasks properly
									ITask newTask = externalizer.readTask(child, tlist, null, null);
									ITaskHandler taskHandler = MylarTaskListPlugin.getDefault().getHandlerForElement(
											newTask);
									if (taskHandler != null) {
										newTask = taskHandler.addTaskToRegistry(newTask);
									}
									tlist.addRootTask(newTask);

									wasRead = true;
									break;
								}
							}
							if (!wasRead && delagatingExternalizer.canReadTask(child)) {
								tlist.addRootTask(delagatingExternalizer.readTask(child, tlist, null, null));
							} else {
								// MylarPlugin.log("Did not read: " +
								// child.getNodeName(), this);
							}
						}
					} catch (Exception e) {
						MylarStatusHandler.log(e, "can't read xml string");
					}
				}
			}
		} catch (Exception e) {
			MylarStatusHandler.log(e, "can't read xml string");
		}
	}

	public String getTaskListXml(TaskList tlist) {
		// TODO make this and writeTaskList use the same base code
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document doc = null;

		try {
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
		} catch (ParserConfigurationException e) {
			MylarStatusHandler.log(e, "could not create document");
			e.printStackTrace();
		}

		Element root = doc.createElement(ELEMENT_TASK_LIST);
		root.setAttribute(ATTRIBUTE_VERSION, VALUE_VERSION);

		for (ITaskListExternalizer externalizer : externalizers) {
			externalizer.createRegistry(doc, root);
		}

		for (ITaskCategory category : tlist.getCategories()) {
			Element element = null;
			for (ITaskListExternalizer externalizer : externalizers) {
				if (externalizer.canCreateElementFor(category))
					element = externalizer.createCategoryElement(category, doc, root);
			}
			if (element == null && delagatingExternalizer.canCreateElementFor(category)) {
				delagatingExternalizer.createCategoryElement(category, doc, root);
			} else if (element == null) {
				MylarStatusHandler.log("Did not externalize: " + category, this);
			}
		}
		for (ITask task : tlist.getRootTasks()) {
			try {
				Element element = null;
				for (ITaskListExternalizer externalizer : externalizers) {
					if (externalizer.canCreateElementFor(task))
						element = externalizer.createTaskElement(task, doc, root);
				}
				if (element == null && delagatingExternalizer.canCreateElementFor(task)) {
					delagatingExternalizer.createTaskElement(task, doc, root);
				} else if (element == null) {
					MylarStatusHandler.log("Did not externalize: " + task, this);
				}
			} catch (Exception e) {
				MylarStatusHandler.log(e, e.getMessage());
			}
		}
		doc.appendChild(root);
		StringWriter sw = new StringWriter();

		Source source = new DOMSource(doc);

		Result result = new StreamResult(sw);

		Transformer xformer = null;
		try {
			xformer = TransformerFactory.newInstance().newTransformer();
			// Transform the XML Source to a Result
			//
			xformer.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sw.toString();
	}

	public void setDelegatingExternalizer(DelegatingLocalTaskExternalizer delagatingExternalizer) {
		this.delagatingExternalizer = delagatingExternalizer;
	}
}
