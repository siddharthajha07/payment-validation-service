/**
 * JAXB model for the ISO 20022 Business Application Header (head.001.001.03).
 *
 * <p>These classes deliberately duplicate some structures that also appear in the
 * pacs.008 package. That is not accidental: the header and the document are separate
 * ISO namespaces, so an element such as {@code BICFI} in the header is a different
 * element from {@code BICFI} in the document and cannot share a binding.
 */
@jakarta.xml.bind.annotation.XmlSchema(
        namespace = com.rbc.paymentvalidation.xml.model.IsoNamespaces.HEAD_001_001_03,
        elementFormDefault = jakarta.xml.bind.annotation.XmlNsForm.QUALIFIED)
package com.rbc.paymentvalidation.xml.model.header;
