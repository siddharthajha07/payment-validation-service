/**
 * JAXB model for the ISO 20022 FI To FI Payment Status Report (pacs.002.001.14).
 *
 * <p>This is the outbound message: the acknowledgement or rejection returned for an
 * incoming pacs.008. Like the inbound model it is a deliberate subset, covering the
 * elements present in the supplied sample responses.
 *
 * <p>Every class here declares an explicit {@code @XmlType(propOrder = ...)}. ISO 20022
 * defines each type as an ordered sequence, so an element written out of order produces a
 * document the receiving bank's schema validation rejects. Relying on field declaration
 * order would happen to work but would leave a correctness property resting on an
 * implementation detail; stating the order makes it a guarantee.
 */
@jakarta.xml.bind.annotation.XmlSchema(
        namespace = com.rbc.paymentvalidation.xml.model.IsoNamespaces.PACS_002_001_14,
        elementFormDefault = jakarta.xml.bind.annotation.XmlNsForm.QUALIFIED)
package com.rbc.paymentvalidation.xml.model.pacs002;
