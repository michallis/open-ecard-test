/****************************************************************************
 * Copyright (C) 2013-2015 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the Open eCard App.
 *
 * GNU General Public License Usage
 * This file may be used under the terms of the GNU General Public
 * License version 3.0 as published by the Free Software Foundation
 * and appearing in the file LICENSE.GPL included in the packaging of
 * this file. Please review the following information to ensure the
 * GNU General Public License version 3.0 requirements will be met:
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package org.openecard.crypto.common.sal;

import iso.std.iso_iec._24727.tech.schema.ConnectionHandleType;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticate;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticateResponse;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticationDataType;
import iso.std.iso_iec._24727.tech.schema.DIDGet;
import iso.std.iso_iec._24727.tech.schema.DIDGetResponse;
import iso.std.iso_iec._24727.tech.schema.DIDScopeType;
import iso.std.iso_iec._24727.tech.schema.DIDStructureType;
import iso.std.iso_iec._24727.tech.schema.Sign;
import iso.std.iso_iec._24727.tech.schema.SignResponse;
import iso.std.iso_iec._24727.tech.schema.TargetNameType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.openecard.bouncycastle.crypto.tls.Certificate;
import org.openecard.bouncycastle.crypto.tls.HashAlgorithm;
import org.openecard.bouncycastle.crypto.tls.SignatureAlgorithm;
import org.openecard.bouncycastle.crypto.tls.SignatureAndHashAlgorithm;
import org.openecard.common.SecurityConditionUnsatisfiable;
import org.openecard.common.WSHelper;
import org.openecard.common.WSHelper.WSException;
import org.openecard.common.interfaces.Dispatcher;
import org.openecard.common.interfaces.DispatcherException;
import org.openecard.common.util.SALFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Wrapper for the sign functionality of generic crypto DIDs.
 *
 * @author Tobias Wich
 * @author Dirk Petrautzki
 * @author Hans-Martin Haase
 */
public class GenericCryptoSigner {

    private static final Logger logger = LoggerFactory.getLogger(GenericCryptoSigner.class);

    private final Dispatcher dispatcher;
    private final SALFileUtils fileUtils;
    private final ConnectionHandleType handle;
    private final String didName;
    private final DIDCertificate didCert;
    private byte[] rawCertData;
    private final Map<String, java.security.cert.Certificate[]> javaCerts;
    private org.openecard.bouncycastle.crypto.tls.Certificate bcCert;

    /**
     * Creates a Generic Crypto signer and defines the card, application and target DID through the parameters.
     *
     * @param dispatcher Dispatcher used to talk to the SAL.
     * @param handle Handle naming the SAL, card and application of the DID. The application connection can change over
     *   time and does not even have to be selected when this method is called.
     * @param cert
     */
    public GenericCryptoSigner(@Nonnull Dispatcher dispatcher, @Nonnull ConnectionHandleType handle,
	    @Nonnull DIDCertificate cert) {
	this.javaCerts = new HashMap<>();
	this.dispatcher = dispatcher;
	this.fileUtils = new SALFileUtils(dispatcher);
	this.handle = handle;
	didName = cert.getDIDName();
	this.didCert = cert;
	this.handle.setCardApplication(cert.getApplicationIdentifier());
    }


    /**
     * Gets the certificate for this generic crypto DID.
     * This function returns the certificate in encoded form exactly as it is saved on the respective token. The
     * certificate is not converted whatsoever.
     *
     * @return Certificate of this DID in encoded form.
     * @throws CredentialPermissionDenied In case the certificate could not be read from the token.
     * @throws IOException In case any other error occurred during the reading of the certificate.
     */
    public synchronized byte[] getCertificateChain() throws CredentialPermissionDenied, IOException {
	if (rawCertData == null) {
	    String dataSetName = didCert.getDataSetName();
	    if (dataSetName != null) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
		    for (java.security.cert.Certificate cert : didCert.buildPath()) {
			try {
			    bout.write(cert.getEncoded());
			} catch (CertificateEncodingException ex) {
			    String msg = "Failed to create byte array representation of certificate.";
			    throw new IOException(msg, ex);
			}
		    }
		} catch (CertificateException ex) {
		    throw new IOException(ex);
		}
		rawCertData = bout.toByteArray();
		//rawCertData = readCertificateDataset(handle, dataSetName);
	    } else {
		throw new IOException("Could not get the certificate data set name.");
	    }
	    if (rawCertData == null) {
		throw new IOException("Failed to read certificate contents.");
	    }
	}
	return rawCertData;
    }

    /**
     * Gets the certificate for this generic crypto DID converted to a Java security certificate.
     * This method is just a convenience function to call the equivalent with the parameter {@code X.509}.
     *
     * @return An array representing the certificate chain (in X.509 format) of this DID.
     * @throws CredentialPermissionDenied In case the certificate could not be read from the token. See
     *   {@link #getCertificateChain()}.
     * @throws IOException In case any other error occurred during the reading of the certificate. See
     *   {@link #getCertificateChain()}.
     * @throws CertificateException In case the certificate could not be converted.
     * @see #getJavaSecCertificateChain(java.lang.String)
     */
    public java.security.cert.Certificate[] getJavaSecCertificateChain() throws CredentialPermissionDenied,
	    CertificateException, IOException {
	return getJavaSecCertificateChain("X.509");
    }
    /**
     * Gets the certificate for this generic crypto DID converted to a Java security certificate.
     * The type parameter is used to determine the requested certificate type. Each certificate type is cached once it is
     * requested.
     *
     * @param certType Certificate type according to <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#CertificateFactory">
     *   CertificateFactory Types</a>
     * @return An array representing the certificate chain of this DID.
     * @throws CredentialPermissionDenied In case the certificate could not be read from the token. See
     *   {@link #getCertificateChain()}.
     * @throws IOException In case any other error occurred during the reading of the certificate. See
     *   {@link #getCertificateChain()}.
     * @throws CertificateException In case the certificate could not be converted.
     */
    @Nonnull
    public java.security.cert.Certificate[] getJavaSecCertificateChain(@Nonnull String certType)
	    throws CredentialPermissionDenied, CertificateException, IOException {
	// is the certificate already available in java.security form?
	if (! javaCerts.containsKey(certType)) {
	    javaCerts.put(certType, toArray(didCert.buildPath()));
	}

	return javaCerts.get(certType);
    }

    private java.security.cert.Certificate[] toArray(Collection<? extends java.security.cert.Certificate> javaCert) {
	java.security.cert.Certificate[] result = new java.security.cert.Certificate[javaCert.size()];
	int i = 0;
	for (java.security.cert.Certificate next : javaCert) {
	    result[i] = next;
	    i++;
	}
	return result;
    }

    /**
     * Gets the certificate for this generic crypto DID converted to a BouncyCastle TLS certificate.
     *
     * @return The certificate chain in BouncyCastle format.
     * @throws CredentialPermissionDenied In case the certificate could not be read from the token. See
     *   {@link #getCertificateChain()}.
     * @throws IOException In case any other error occurred during the reading of the certificate. See
     *   {@link #getCertificateChain()}.
     * @throws CertificateException In case the certificate could not be converted.
     */
    @Nonnull
    public org.openecard.bouncycastle.crypto.tls.Certificate getBCCertificateChain() throws CredentialPermissionDenied,
	    CertificateException, IOException {
	// is the certificate already available in BC form?
	if (bcCert == null) {
	    //byte[] certs = getCertificateChain();
	    bcCert = convertToCertificate();
	}

	return bcCert;
    }

    public String getAlgorithm() {
        String algorithm = null;

        try {
            DIDGet didGet = new DIDGet();
            didGet.setConnectionHandle(handle);
            didGet.setDIDName(didName);
            DIDGetResponse didGetResponse = (DIDGetResponse) dispatcher.deliver(didGet);
            WSHelper.checkResult(didGetResponse);
            CryptoMarkerType cryptoMarker = new CryptoMarkerType(didGetResponse.getDIDStructure().getDIDMarker());

            algorithm = cryptoMarker.getAlgorithmInfo().getAlgorithmIdentifier().getAlgorithm();
        } catch (WSException | DispatcherException | InvocationTargetException ex) {
            logger.error("Error getting algorithm.", ex);
        }

        return algorithm;
    }
    
    /**
     * Signs the given hash with the DID represented by this instance.
     *
     * @param hash The hash that should be signed.
     * @return Signature of the given hash.
     * @throws SignatureException In case the signature could not be created. Causes are most likely problems in the
     *   SAL, such as a removed card.
     * @throws CredentialPermissionDenied In case the signature could not be performed by the token due to missing
     *   permissions.
     */
    public byte[] sign(@Nonnull byte[] hash) throws SignatureException, CredentialPermissionDenied {
	try {
	    fileUtils.selectApplication(didCert.getApplicationIdentifier(), handle);
	    handle.setCardApplication(didCert.getApplicationIdentifier());
	    TargetNameType target = new TargetNameType();
	    target.setDIDName(didName);
	    performMissingAuthentication(target);

	    Sign sign = new Sign();
	    sign.setMessage(hash);
	    sign.setDIDName(didName);
	    sign.setDIDScope(DIDScopeType.LOCAL);
	    sign.setConnectionHandle(handle);
	    SignResponse res = (SignResponse) dispatcher.deliver(sign);
	    WSHelper.checkResult(res);

	    byte[] sig = res.getSignature();
	    if (sig == null) {
		logger.error("Failed to create signature for TLS connection.");
		return new byte[] {};
	    } else {
		return sig;
	    }
	} catch (InvocationTargetException e) {
	    logger.error("Signature generation failed.", e);
	    throw new SignatureException(e);
	} catch (DispatcherException e) {
	   logger.error("Signature generation failed.", e);
	    throw new SignatureException(e);
	} catch (WSException e) {
	    logger.error("Signature generation failed.", e);
	    throw new SignatureException(e);
	} catch (SecurityConditionUnsatisfiable e) {
	    logger.error("Signature generation failed.", e);
	    throw new CredentialPermissionDenied(e);
	}
    }

    private Certificate convertToCertificate() throws CertificateException {
	try {
	    java.security.cert.Certificate[] jCertificates = getJavaSecCertificateChain();
	    org.openecard.bouncycastle.asn1.x509.Certificate[] certs
		    = new org.openecard.bouncycastle.asn1.x509.Certificate[jCertificates.length];
	    for (int i = 0; i < jCertificates.length; i++) {
		org.openecard.bouncycastle.asn1.x509.Certificate x509Certificate
			= org.openecard.bouncycastle.asn1.x509.Certificate.getInstance(jCertificates[i].getEncoded());

		if (x509Certificate == null) {
		    throw new CertificateException("Couldn't convert to x509Certificate.");
		}
		certs[i] = x509Certificate;
	    }
	    Certificate cert = new Certificate(certs);
	    return cert;
	} catch (CredentialPermissionDenied | IOException ex) {
	    throw new CertificateException(ex);
	}
    }

    private void performMissingAuthentication(TargetNameType target) throws DispatcherException, WSException,
	    InvocationTargetException, SecurityConditionUnsatisfiable {
	// get unauthenticated DID
	ACLResolver resolver = new ACLResolver(dispatcher, handle);
	List<DIDStructureType> missingDIDs = resolver.getUnsatisfiedDIDs(target);

	// authenticate those DIDs
	for (DIDStructureType did : missingDIDs) {
	    DIDAuthenticate req = new DIDAuthenticate();
	    req.setConnectionHandle(handle);
	    req.setDIDName(did.getDIDName());
	    req.setDIDScope(did.getDIDScope());

	    DIDAuthenticationDataType authData = new DIDAuthenticationDataType();
	    authData.setProtocol(did.getDIDMarker().getProtocol());
	    // TODO: no further content does not work for all protocols
	    // however it does work for PIN Compare, which seems enough so far
	    req.setAuthenticationProtocolData(authData);

	    DIDAuthenticateResponse res = (DIDAuthenticateResponse) dispatcher.deliver(req);
	    WSHelper.checkResult(res);
	}
    }

    public SignatureAndHashAlgorithm getSignatureAndHashAlgorithm() {
	SignatureAndHashAlgorithm sigAndHash = new SignatureAndHashAlgorithm(HashAlgorithm.sha256, SignatureAlgorithm.rsa);
	try {
	    DIDGet didGet = new DIDGet();
	    didGet.setConnectionHandle(handle);
	    didGet.setDIDName(didName);
	    didGet.setDIDScope(DIDScopeType.LOCAL);
	    DIDGetResponse response = (DIDGetResponse) dispatcher.deliver(didGet);
	    CryptoMarkerType cryptoMarker = new CryptoMarkerType(response.getDIDStructure().getDIDMarker());
	    String algorithm = cryptoMarker.getAlgorithmInfo().getAlgorithmIdentifier().getAlgorithm();
	    sigAndHash = AlgorithmResolver.getSignatureAndHashFromAlgorithm(algorithm);

	    if (sigAndHash == null) {
		throw new IllegalArgumentException("Illegal oid for the signature algorithm.");
	    }
	} catch (DispatcherException ex) {
	    logger.error("Failed to get DID for DIDName: {}.", didName, ex);
	} catch (InvocationTargetException ex) {
	    logger.error("Failed to get DID for DIDName: {}.", didName, ex);
	} catch (IllegalArgumentException ex) {
	    logger.error("Failed to find a valid SignatureAndHashAlgorithm object for the OID used in the CryptoMarker "
		    + "of the DID with the DIDName: {}.", didName, ex);
	}

	return sigAndHash;
    }

}
