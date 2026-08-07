package com.rbc.paymentvalidation.xml.signature;

import com.rbc.paymentvalidation.xml.model.IsoNamespaces;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Applies and verifies the enveloped XML signature on a status report.
 *
 * The signature sits inside the document it signs, in AppHdr/Sgntr. The reference URI is empty
 * (the whole document) with the enveloped transform removing the signature itself from what is
 * hashed, since it cannot cover its own value. Canonicalisation matters because two documents
 * can mean the same thing and differ byte for byte, which is also why a signed document must
 * never be reformatted.
 *
 * KeyInfo includes the certificate so a receiver can verify without being sent it first.
 * Whether they trust it is a separate question. XMLSignatureFactory is not documented as
 * thread-safe so one is made per call.
 */
@Component
public class XmlSignatureService {

    private static final Logger log = LoggerFactory.getLogger(XmlSignatureService.class);

    private static final String RSA_SHA256 =
            "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    private final PrivateKey privateKey;
    private final X509Certificate certificate;

    public XmlSignatureService(SigningProperties properties) {
        try (InputStream keystoreStream =
                     new ClassPathResource(properties.keystoreLocation()).getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(keystoreStream, properties.keystorePassword().toCharArray());

            this.privateKey = (PrivateKey) keyStore.getKey(properties.keyAlias(),
                    properties.keyPassword().toCharArray());
            this.certificate = (X509Certificate) keyStore.getCertificate(properties.keyAlias());

            if (privateKey == null || certificate == null) {
                throw new IllegalStateException(
                        "Signing key '%s' not found in keystore".formatted(properties.keyAlias()));
            }
            log.info("Loaded signing certificate for {}",
                    certificate.getSubjectX500Principal().getName());
        } catch (Exception e) {
            // Failing at startup is intended. A service that cannot sign cannot produce a
            // valid response, and should not accept traffic it can only answer incorrectly.
            throw new IllegalStateException("Unable to load the signing key", e);
        }
    }

    /**
     * Signs the document in place, inserting the signature into AppHdr/Sgntr.
     *
     * The document must not be modified afterwards — not even reformatted — or the
     * signature will no longer verify.
     *
     * @param document the marshalled status report
     * @throws XmlSignatureException if the document cannot be signed
     */
    public void sign(Document document) {
        try {
            Element signatureParent = createSignatureContainer(document);

            XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");

            // URI "" means the whole document; the enveloped transform excludes the
            // signature element itself from what is digested.
            Reference reference = factory.newReference("",
                    factory.newDigestMethod(DigestMethod.SHA256, null),
                    List.of(factory.newTransform(Transform.ENVELOPED,
                                    (TransformParameterSpec) null),
                            factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                                    (C14NMethodParameterSpec) null)),
                    null, null);

            SignedInfo signedInfo = factory.newSignedInfo(
                    factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                            (C14NMethodParameterSpec) null),
                    factory.newSignatureMethod(RSA_SHA256, null),
                    List.of(reference));

            XMLSignature signature = factory.newXMLSignature(signedInfo, keyInfo(factory));
            signature.sign(new DOMSignContext(privateKey, signatureParent));
        } catch (Exception e) {
            throw new XmlSignatureException("Unable to sign the status report", e);
        }
    }

    /**
     * Verifies the signature on a document against this service's own certificate.
     *
     * Used by the tests to prove the signature is genuinely valid over the bytes
     * produced, rather than merely present.
     *
     * @return true if a signature is present and valid
     */
    public boolean verify(Document document) {
        try {
            NodeList signatures = document.getElementsByTagNameNS(
                    XMLSignature.XMLNS, "Signature");
            if (signatures.getLength() == 0) {
                return false;
            }

            DOMValidateContext context = new DOMValidateContext(publicKey(), signatures.item(0));
            return XMLSignatureFactory.getInstance("DOM")
                    .unmarshalXMLSignature(context)
                    .validate(context);
        } catch (Exception e) {
            log.warn("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /** @return the certificate presented in outbound signatures. */
    public X509Certificate getCertificate() {
        return certificate;
    }

    public PublicKey publicKey() {
        return certificate.getPublicKey();
    }

    /**
     * Creates the Sgntr element the signature is placed inside.
     *
     * Sgntr belongs to the business header namespace and is the last element of
     * the header, which is where the ISO business application header defines it.
     *
     * createElementNS produces a node that knows its namespace but carries no
     * xmlns attribute. Canonicalisation works from the attributes actually present,
     * so the signature would be computed over a form of this element with no namespace
     * declaration — while the serialiser, needing to emit correct XML, writes one anyway.
     * The bytes the receiver parses would then differ from the bytes that were signed, and
     * the signature would fail to verify for no visible reason.
     *
     * Declaring it here makes the in-memory document and its serialised form agree. This
     * is a well-known trap when signing a DOM built in code rather than parsed from text,
     * and signatureSurvivesSerialisation in the tests exists to keep it fixed.
     */
    private Element createSignatureContainer(Document document) {
        NodeList headers = document.getElementsByTagNameNS(
                IsoNamespaces.MONTRAN_ENVELOPE, "AppHdr");
        if (headers.getLength() == 0) {
            throw new XmlSignatureException(
                    "Document has no AppHdr to place the signature in", null);
        }

        Element header = (Element) headers.item(0);
        Element signatureContainer = document.createElementNS(
                IsoNamespaces.HEAD_001_001_03, "Sgntr");
        signatureContainer.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                XMLConstants.XMLNS_ATTRIBUTE, IsoNamespaces.HEAD_001_001_03);
        header.appendChild(signatureContainer);
        return signatureContainer;
    }

    private KeyInfo keyInfo(XMLSignatureFactory factory) {
        KeyInfoFactory keyInfoFactory = factory.getKeyInfoFactory();
        X509Data x509Data = keyInfoFactory.newX509Data(List.of(
                certificate.getSubjectX500Principal().getName(),
                certificate));
        return keyInfoFactory.newKeyInfo(List.of(x509Data));
    }
}
