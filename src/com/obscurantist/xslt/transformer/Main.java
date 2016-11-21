package com.obscurantist.xslt.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Map<String, String> xpathsList = new LinkedHashMap<String, String>();
		XMLTransformer xmlTransformer = new XMLTransformer();
		Map<String, String> namespacesMap = new LinkedHashMap<String, String>();
		try {
			xpathsList = readPropertiesFileAsMap("XPATHList.properties");
			namespacesMap = readPropertiesFileAsMap("namespaces.properties");
			Document xmlOutput = xmlTransformer.transform(xpathsList, namespacesMap);
			outputDoc(xmlOutput);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}

	public static void outputDoc(Document doc) {
		DOMSource source = new DOMSource(doc);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		StringWriter sw = new StringWriter();
		try {
			transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.transform(source, new StreamResult(sw));
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(sw.toString());
	}

	public static ArrayList<String> readFileAsList(String filePath) throws FileNotFoundException {
		File file = new File(filePath);
		System.out.println("Reading from " + file.getAbsolutePath());
		ArrayList<String> list = new ArrayList<String>();
		Scanner s = new Scanner(file);
		while (s.hasNext()) {
			list.add(s.next());
		}
		s.close();
		return list;
	}

	public static ArrayList<String> readFileAsXPathsList(String filePath) throws FileNotFoundException {
		File file = new File(filePath);
		System.out.println("Reading from " + file.getAbsolutePath());
		ArrayList<String> list = new ArrayList<String>();
		Scanner s = new Scanner(file);
		while (s.hasNext()) {
			String line = s.next();
			if (!line.startsWith("/")) {
				line = "/" + line;
			}
			list.add(line);
		}
		s.close();
		return list;
	}

	public static Map<String, String> readPropertiesFileAsMap(String fileName) throws IOException {
		LinkedHashMap<String, String> propertyMap = new LinkedHashMap<String, String>();
		Properties properties = null;
		properties = getPropertiesObject(fileName);
		for (final String name : properties.stringPropertyNames()) {
			propertyMap.put(name, properties.getProperty(name));
		}
		return propertyMap;
	}

	public static Properties getPropertiesObject(String fileName) throws IOException {
		Properties properties = new Properties();
		System.out.println("Reading from " + (new File(fileName)).getAbsolutePath());
		InputStream fileInputStream = new FileInputStream(fileName);
		properties.load(fileInputStream);
		fileInputStream.close();
		return properties;
	}

}
