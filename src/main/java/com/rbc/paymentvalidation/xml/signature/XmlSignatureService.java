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
 * Applies and verifies the enveloped XML digital signature on a status report.
 *
 * <h2>What "enveloped" means and why the details matter</h2>
 * The signature sits inside the document it signs, in {@code AppHdr/Sgntr}. Three choices
 * make that work, and each one silently invalidates the signature if it is wrong.
 *
 * <p><strong>The reference URI is the empty string</strong>, which means "everything in
 * this document". Paired with it is the {@code enveloped-signature} transform, which
 * removes the signature element itself from what is hashed — without it the signature
 * would have to cover its own value, which is impossible.
 *
 * <p><strong>Canonicalisation.</strong> Two XML documents can be semantically identical yet
 * differ byte for byte: attribute order, namespace declarations, whitespace. A signature
 * is over bytes, so both signer and verifier must agree on one canonical byte form first.
 * That is what C14N is for, and it is why a signed document must never be reformatted.
 *
 * <p><strong>Nothing may touch the document after signing.</strong> Pretty-printing a
 * signed document is the classic way to break it: the XML still means the same thing, the
 * bytes differ, and the digest no longer matches.
 *
 * <h2>Why the certificate is included</h2>
 * {@code KeyInfo} carries the subject name and issuer serial, matching the supplied
 * samples, and also the certificate itself. Including it lets a receiver verify the
 * signature without having been given the certificate in advance — they must still decide
 * whether they <em>trust</em> it, which is a separate question answered by checking the
 * issuer against their own trust store.
 *
 * <h2>Thread safety</h2>
 * {@link XMLSignatureFactory} instances are not documented as thread-safe, so one is
 * created per call. The key material is immutable and loaded once at startup.
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
     * Signs the document in place, inserting the signature into {@code AppHdr/Sgntr}.
     *
     * <p>The document must not be modified afterwards — not even reformatted — or the
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
     * <p>Used by the tests to prove the signature is genuinely valid over the bytes
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
     * Creates the {@code Sgntr} element the signature is placed inside.
     *
     * <p>{@code Sgntr} belongs to the business header namespace and is the last element of
     * the header, which is where the ISO business application header defines it.
     *
     * <h3>Why the namespace is declared explicitly</h3>
     * {@code createElementNS} produces a node that knows its namespace but carries no
     * {@code xmlns} attribute. Canonicalisation works from the attributes actually present,
     * so the signature would be computed over a form of this element with no namespace
     * declaration — while the serialiser, needing to emit correct XML, writes one anyway.
     * The bytes the receiver parses would then differ from the bytes that were signed, and
     * the signature would fail to verify for no visible reason.
     *
     * <p>Declaring it here makes the in-memory document and its serialised form agree. This
     * is a well-known trap when signing a DOM built in code rather than parsed from text,
     * and {@code signatureSurvivesSerialisation} in the tests exists to keep it fixed.
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
