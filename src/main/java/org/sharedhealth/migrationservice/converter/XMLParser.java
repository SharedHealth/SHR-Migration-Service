package org.sharedhealth.migrationservice.converter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class XMLParser {

    private static final String DIAGNOSTIC_ORDER_SECTION_DISPLAY = "Diagnostic Order";
    private static final String DIAGNOSTIC_ORDER_TAG_NAME = "DiagnosticOrder";

    private static final String PROCEDURE_REQUEST_SECTION_DISPLAY = "Procedure Request";
    private static final String PROCEDURE_REQUEST_TAG_NAME = "ProcedureRequest";

    public static String removeExistingDiagnosticOrderFromBundleContent(String dstu2BundleContent) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        List<Element> entriesToRemove = new ArrayList<>();
        DocumentBuilderFactory dbf =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(dstu2BundleContent));

        Document doc = db.parse(is);
        Element bundle = (Element) doc.getChildNodes().item(0);
        NodeList entries = bundle.getElementsByTagName("entry");
        Element compositionElement = null;
        int numberOfEntries = entries.getLength();
        for (int i = 0; i < numberOfEntries; i++) {
            Element entry = (Element) entries.item(i);
            if (entry.getParentNode().equals(bundle)) {
                NodeList entryChildren = entry.getChildNodes();
                for (int j = 0; j < entryChildren.getLength(); j++) {
                    if (!(entryChildren.item(j) instanceof Element)) continue;
                    Element entryChild = (Element) entryChildren.item(j);
                    if (!entryChild.getTagName().equals("resource")) continue;
                    NodeList resourceChildren = entryChild.getChildNodes();
                    for (int k = 0; k < resourceChildren.getLength(); k++) {
                        if (!(resourceChildren.item(k) instanceof Element)) continue;
                        Element resourceChild = (Element) resourceChildren.item(k);
                        if (resourceChild.getTagName().equals("Composition")) {
                            compositionElement = resourceChild;
                        }
                        if (resourceChild.getTagName().equals(DIAGNOSTIC_ORDER_TAG_NAME)) {
                            entriesToRemove.add(entry);
                        }
                        if (resourceChild.getTagName().equals(PROCEDURE_REQUEST_TAG_NAME)) {
                            entriesToRemove.add(entry);
                        }
                    }
                }
            }
        }

        for (Element entryToRemove : entriesToRemove) {
            bundle.removeChild(entryToRemove);
        }

        List<Element> sectionsToRemove = new ArrayList<>();
        NodeList compositionSections = compositionElement.getElementsByTagName("section");
        int numberOfCompositionSections = compositionSections.getLength();
        for (int i = 0; i < numberOfCompositionSections; i++) {
            Element section = (Element) compositionSections.item(i);
            Node displayNode = section.getElementsByTagName("display").item(0);
            if (displayNode == null) continue;
            if (DIAGNOSTIC_ORDER_SECTION_DISPLAY.equals(displayNode.getAttributes().item(0).getNodeValue())) {
                sectionsToRemove.add(section);
            }
            if (PROCEDURE_REQUEST_SECTION_DISPLAY.equals(displayNode.getAttributes().item(0).getNodeValue())) {
                sectionsToRemove.add(section);
            }
        }
        for (Element sectionToRemove : sectionsToRemove) {
            compositionElement.removeChild(sectionToRemove);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
        return result.getWriter().toString();
    }

}
