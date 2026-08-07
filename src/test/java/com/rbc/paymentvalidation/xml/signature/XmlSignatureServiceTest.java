package com.rbc.paymentvalidation.xml.signature;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.mapper.Pacs002Factory;
import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.xml.Pacs002Marshaller;
import javax.xml.crypto.dsig.XMLSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests for the enveloped XML digital signature.
 *
 * <p>The important tests here are the last two. Proving a signature is <em>present</em>
 * proves very little; proving it <em>verifies</em>, and that it stops verifying once the
 * document is altered, is what shows the signature actually protects anything.
 */
class XmlSignatureServiceTest {

    private static final SigningProperties PROPERTIES = new SigningProperties(
            "keystore/signing-keystore.p12", "changeit", "payment-signing", "changeit");

    private final XmlSignatureService signatureService = new XmlSignatureService(PROPERTIES);
    private final Pacs002Marshaller marshaller = new Pacs002Marshaller();
    private final Pacs002Factory factory = new Pacs002Factory(ValidationFixtures.FIXED_CLOCK);

    private Document signedResponse() {
        Document document = marshaller.toDocument(
                factory.accept(ValidationFixtures.validMessage()));
        signatureService.sign(document);
        return document;
    }

    @Test
    @DisplayName("places the signature inside the business header's Sgntr element")
    void placesSignatureInsideSgntr() {
        Document document = signedResponse();

        Element signature = (Element) document
                .getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);

        assertThat(signature).isNotNull();
        assertThat(signature.getParentNode().getLocalName()).isEqualTo("Sgntr");
    }

    @Test
    @DisplayName("signs with RSA-SHA256 over the whole document")
    void signsWithExpectedAlgorithms() {
        // URI "" means the entire document; the enveloped transform removes the signature
        // itself from what is digested, since it cannot cover its own value.
        String xml = marshaller.toXml(signedResponse());

        assertThat(xml).contains("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        assertThat(xml).contains("http://www.w3.org/2001/04/xmlenc#sha256");
        assertThat(xml).contains("http://www.w3.org/2000/09/xmldsig#enveloped-signature");
        assertThat(xml).contains("<Reference URI=\"\">");
    }

    @Test
    @DisplayName("publishes the certificate so a receiver can verify without prior exchange")
    void publishesCertificate() {
        String xml = marshaller.toXml(signedResponse());

        assertThat(xml).contains("X509SubjectName");
        assertThat(xml).contains("CN=CBANK0IPS-SIGNING");
        assertThat(xml).contains("X509Certificate");
    }

    @Test
    @DisplayName("produces a signature that verifies")
    void producesAVerifiableSignature() {
        assertThat(signatureService.verify(signedResponse())).isTrue();
    }

    @Test
    @DisplayName("stops verifying once a single character of the document is altered")
    void detectsTampering() {
        // This is the whole point of signing. Changing an accepted status to a rejected one
        // is exactly the alteration an attacker would attempt, and the digest no longer
        // matches the document once they do.
        Document document = signedResponse();
        Element groupStatus = (Element) document.getElementsByTagNameNS(
                "urn:iso:std:iso:20022:tech:xsd:pacs.002.001.14", "GrpSts").item(0);
        groupStatus.setTextContent("RJCT");

        assertThat(signatureService.verify(document)).isFalse();
    }

    @Test
    @DisplayName("produces a signature that still verifies after a round trip through text")
    void signatureSurvivesSerialisation() {
        // The signature that matters is the one the receiver checks, and the receiver has
        // only the bytes on the wire. Verifying the in-memory document proves less than it
        // appears to: an element created in code carries no xmlns attribute, so the
        // canonical form used for signing can differ from the form the serialiser writes,
        // and the signature then fails on arrival while verifying perfectly at home.
        String xml = marshaller.toXml(signedResponse());

        assertThat(signatureService.verify(
                new com.rbc.paymentvalidation.xml.SecureXmlParser(1_048_576)
                        .parse(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .isTrue();
    }

    @Test
    @DisplayName("reports an unsigned document as unverified rather than valid")
    void treatsUnsignedDocumentAsUnverified() {
        Document unsigned = marshaller.toDocument(
                factory.accept(ValidationFixtures.validMessage()));

        assertThat(signatureService.verify(unsigned)).isFalse();
    }
}
