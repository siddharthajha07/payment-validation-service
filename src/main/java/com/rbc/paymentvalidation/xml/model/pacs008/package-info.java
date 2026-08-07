/**
 * JAXB model for the ISO 20022 FI To FI Customer Credit Transfer (pacs.008.001.12).
 *
 * <p>This is a deliberate subset of the full ISO message: only the elements this service
 * validates or persists are modelled. Unmapped elements are ignored during unmarshalling
 * rather than causing a failure, so a message carrying optional fields we do not use is
 * still processed. Structural strictness is enforced separately, by XSD validation.
 */
@jakarta.xml.bind.annotation.XmlSchema(
        namespace = com.rbc.paymentvalidation.xml.model.IsoNamespaces.PACS_008_001_12,
        elementFormDefault = jakarta.xml.bind.annotation.XmlNsForm.QUALIFIED)
package com.rbc.paymentvalidation.xml.model.pacs008;
