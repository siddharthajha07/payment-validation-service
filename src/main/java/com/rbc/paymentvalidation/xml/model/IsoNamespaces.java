package com.rbc.paymentvalidation.xml.model;

/** XML namespace URIs for the message types this service handles. */
public final class IsoNamespaces {

    /** Montran envelope that wraps the business header and the ISO document. */
    public static final String MONTRAN_ENVELOPE = "urn:montran:message.01";

    /** ISO 20022 Business Application Header. */
    public static final String HEAD_001_001_03 = "urn:iso:std:iso:20022:tech:xsd:head.001.001.03";

    /** FI To FI Customer Credit Transfer — the inbound payment instruction. */
    public static final String PACS_008_001_12 = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.12";

    /** FI To FI Payment Status Report — the outbound acknowledgement or rejection. */
    public static final String PACS_002_001_14 = "urn:iso:std:iso:20022:tech:xsd:pacs.002.001.14";

    /** W3C XML Digital Signature. */
    public static final String XML_DSIG = "http://www.w3.org/2000/09/xmldsig#";

    private IsoNamespaces() {
        // Constants holder; not instantiable.
    }
}
