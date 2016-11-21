package com.obscurantist.xslt.transformer;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XMLTransformer {

	private Document createTargetDocument(Map<String, String> mappings, Map<String, String> namespaces) throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		Document document = dbFactory.newDocumentBuilder().newDocument();
		
		for (Entry<String, String> entry : mappings.entrySet()) {

			String targetXpath = entry.getKey();

			String[] tokens = targetXpath.split("/");
			List<String> tokensList = new ArrayList<String>();
			for (String token : tokens) {
				if (token != null && !token.trim().equals(""))
					tokensList.add(token.trim());
			}

			String previousXpath = "";
			Element parentElement = null;

			for (String token : tokensList) {

				String currentXpath = previousXpath + "/" + token;

				if (token.contains("[")) {

					String neededNode = token.split("\\[")[0];
					String indexStr = token.split("\\[")[1].split("\\]")[0];

					Integer index = Integer.valueOf(indexStr.trim());
					NodeList nodes = getNodeList(previousXpath + "/" + neededNode, document);

					int resultCount = nodes.getLength();

					if (resultCount < index) {

						Element currentParent = null;
						for (int i = resultCount + 1; i <= index; i++) {
							Element el = document.createElement(neededNode);
							if (parentElement != null) {
								parentElement.appendChild(el);
							}
							if (i == index) {
								currentParent = el;
							}
						}

						parentElement = currentParent;
					}
					else {

						NodeList nodeList = getNodeList(currentXpath, document);
						parentElement = (Element) nodeList.item(0);
					}

				}
				else {

					NodeList nodes = getNodeList(currentXpath, document);
					int resultCount = nodes.getLength();
					if (resultCount > 0) {
						if (nodes.item(0) instanceof Element) {
							parentElement = (Element) nodes.item(0);
						}
					}
					else {

						if (token.contains("@")) {
							Attr attr = document.createAttribute(token.replace("@", "").trim());
							if (parentElement != null) {
								parentElement.setAttributeNode(attr);
							}
						}
						else {

							if (parentElement == null) {
								Element root = document.createElement(token);
								if(namespaces != null && namespaces.size() > 0) {
									addLANamespaces(root, namespaces);
								}
								document.appendChild(root);
								parentElement = root;
							}
							else {
								Element el = document.createElement(token);
								parentElement.appendChild(el);
								parentElement = el;
							}
						}
					}

				}
				previousXpath = currentXpath;
			}
		}
		return document;
	}
	
	private void addLANamespaces(Element root, Map<String, String> namespaces) {
		for(Entry<String, String> namespace : namespaces.entrySet()) {
			root.setAttribute(namespace.getKey(), namespace.getValue());
		}
	}
	
	public Document transform(Map<String, String> finalMappings, Map <String, String> namespaces) throws Exception {
		if(finalMappings == null) {
			return createTargetDocument(new TreeMap<String, String>(), namespaces);
		}
		
		Document targetDoc = createTargetDocument(finalMappings, namespaces);
		
		for (Entry<String, String> entry : finalMappings.entrySet()) {

			String path = entry.getKey();
			String value = entry.getValue();

			NodeList nodes = getNodeList(path, targetDoc);

			if (nodes.item(0) instanceof Attr) {

				((Attr) nodes.item(0)).setNodeValue(value);

			}
			else if (nodes.item(0) instanceof Element) {
				((Element) nodes.item(0)).setTextContent(value);

			}
		}
		return targetDoc;
	}

	public Map<String, String> generateCompleteXpaths(Map<String, String> mappings, Document doc) throws Exception {

		Map<String, String> finalMappings = new LinkedHashMap<String, String>();

		for (Entry<String, String> entry : mappings.entrySet()) {

			String targetXpath = entry.getKey();
			String sourceXpath = entry.getValue();

			if (sourceXpath.contains("[")) {
				Map<String, String> subMap = getArrayXPaths(targetXpath, sourceXpath, doc);
				finalMappings.putAll(subMap);

			}
			else {
				if(targetXpath.contains("[")) {
					targetXpath = targetXpath.replace("[a]", "");
				}
				String sourceValue = getElementValue(sourceXpath, doc);
				if (sourceValue != null && !sourceValue.trim().equals("")) {
					finalMappings.put(targetXpath, sourceValue);
				}
			}

		}
		return finalMappings;
	}

	private Map<String, String> getArrayXPaths(String targetXpath, String sourceXpath, Document doc) throws Exception {

		Map<String, String> subMap = new LinkedHashMap<String, String>();
		if (sourceXpath.contains("[a]")) {

			String xpath = sourceXpath.split("\\[a\\]")[0];
			NodeList nodeList = getNodeList(xpath, doc);
			if (nodeList != null) {
			String finalSourceXpath = null;
			String finalTargetXpath = null;
				for (int i = 1; i <= nodeList.getLength(); i++) {
					finalSourceXpath = sourceXpath.replace("[a]", "[" + i + "]");
					finalTargetXpath = targetXpath.replace("[a]", "[" + i + "]");
					if (!finalSourceXpath.contains("[b]")) {
						String sourceValue = getElementValue(finalSourceXpath, doc);
						if (sourceValue != null && !sourceValue.trim().equals("")) {
							subMap.put(finalTargetXpath, sourceValue);
						}
					}
					else {
						String iteratedXPathB = finalSourceXpath.split("\\[b\\]")[0];
						NodeList nodeListB = getNodeList(iteratedXPathB, doc);

						if (nodeListB != null) {
							for (int j = 1; j <= nodeListB.getLength(); j++) {
								String sourceXPath = finalSourceXpath.replace("[b]", "[" + j + "]");
								String targetXPath = finalTargetXpath.replace("[b]", "[" + j + "]");
								String sourceValue = getElementValue(sourceXPath, doc);
								if (sourceValue != null && !sourceValue.trim().equals("")) {
									subMap.put(targetXPath, sourceValue);
								}
							}
						}
					}
				}
			}
		}

		return subMap;
	}
	
	private String formatXPath(String rawXPath){
		
		String[] chunks = rawXPath.split("/");
		String xPath = "";
		for(String chunk : chunks) {
			StringBuilder sb = new StringBuilder();
			sb.append("/");
			if(chunk.contains(":")) {
				String name = "";
				sb.append("*[local-name()='");
				if(chunk.contains("[")) {
					name = chunk.substring(chunk.indexOf(":") + 1, chunk.indexOf("["));
					sb.append(name);
					sb.append("']");
					sb.append(chunk.substring(chunk.indexOf("[")));
				} else {
					name = chunk.substring(chunk.indexOf(":") + 1);
					sb.append(name);
					sb.append("']");
				}
				
			} else {
				sb.append(chunk);
			}
			xPath += sb.toString();
		}
		return xPath;
		
	}
	
	private NodeList getNodeList(String currentXpath, Document document) throws Exception {
		String xPath = formatXPath(currentXpath);
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expression = xpath.compile(xPath);
		NodeList nodeList = (NodeList) expression.evaluate(document, XPathConstants.NODESET);


		return nodeList;

	}

	private String getElementValue(String xPath, Document document) throws Exception {
		String value = "";
		if(StringUtils.isNotBlank(xPath)) {
			String formatedXPath = formatXPath(xPath); 
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression expression = xpath.compile(formatedXPath);
			value = expression.evaluate(document);
		}
		return value;
	}

}
