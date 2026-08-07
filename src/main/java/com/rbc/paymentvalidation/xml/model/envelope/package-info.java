/**
 * JAXB model for the Montran envelope that wraps the business header and ISO document.
 *
 * <p>The {@code @XmlSchema} annotation below applies the envelope namespace to every
 * class in this package, so individual classes do not repeat it.
 * {@code elementFormDefault = QUALIFIED} means nested elements are also in that
 * namespace, which matches how the supplied samples are written.
 */
@jakarta.xml.bind.annotation.XmlSchema(
        namespace = com.rbc.paymentvalidation.xml.model.IsoNamespaces.MONTRAN_ENVELOPE,
        elementFormDefault = jakarta.xml.bind.annotation.XmlNsForm.QUALIFIED)
package com.rbc.paymentvalidation.xml.model.envelope;
