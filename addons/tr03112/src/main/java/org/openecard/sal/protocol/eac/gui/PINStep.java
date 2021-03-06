/****************************************************************************
 * Copyright (C) 2012-2014 ecsec GmbH.
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

package org.openecard.sal.protocol.eac.gui;

import org.openecard.common.I18n;
import org.openecard.gui.definition.InputInfoUnit;
import org.openecard.sal.protocol.eac.anytype.PACEMarkerType;
import org.openecard.gui.definition.PasswordField;
import org.openecard.gui.definition.Step;
import org.openecard.gui.definition.Text;
import org.openecard.sal.protocol.eac.EACData;


/**
 * PIN GUI step for EAC.
 * This GUI step behaves differently
 *
 * @author Tobias Wich
 * @author Moritz Horsch
 */
public class PINStep extends Step {

    private static final I18n langEac = I18n.getTranslation("eac");
    private static final I18n langPace = I18n.getTranslation("pace");
    // step id
    public static final String STEP_ID = "PROTOCOL_EAC_GUI_STEP_PIN";
    // GUI translation constants
    private static final String TITLE = "step_pace_title";
    private static final String STEP_DESCRIPTION = "step_pace_step_description";
    private static final String DESCRIPTION = "step_pace_description";
    private static final String DESCRIPTION_NATIVE = "step_pace_native_description";
    private static final String NOTICE = "eac_forward_notice";
    private static final String TRANSACTION_INFO = "transaction_info";
    // GUI element IDs
    public static final String PIN_FIELD = "PACE_PIN_FIELD";
    public static final String CAN_FIELD = "PACE_CAN_FIELD";

    private static final String CAN_NOTICE_ID = "PACE_CAN_NOTICE";
    private static final String PIN_ATTEMPTS_ID = "PACE_PIN_ATTEMPTS";

    private final String pinType;
    private final PACEMarkerType paceMarker;

    public PINStep(EACData eacData, boolean capturePin, PACEMarkerType paceMarker) {
	super(STEP_ID);
	this.pinType = langPace.translationForKey(eacData.passwordType);
	this.paceMarker = paceMarker;
	setTitle(langPace.translationForKey(TITLE, pinType));
	setDescription(langPace.translationForKey(STEP_DESCRIPTION));

	// TransactionInfo
	String transactionInfo = eacData.transactionInfo;
	if (transactionInfo != null) {
	    Text transactionInfoField = new Text();
	    transactionInfoField.setText(langEac.translationForKey(TRANSACTION_INFO, transactionInfo));
	    getInputInfoUnits().add(transactionInfoField);
	}

	// create step elements
	if (capturePin) {
	    addSoftwareElements();
	} else {
	    addTerminalElements();
	}
    }

    public static Step createDummy(String passwordType) {
	Step s = new Step(STEP_ID);
	String pinType = langPace.translationForKey(passwordType);
	s.setTitle(langPace.translationForKey(TITLE, pinType));
	s.setDescription(langPace.translationForKey(STEP_DESCRIPTION));
	return s;
    }

    private void addSoftwareElements() {
	setResetOnLoad(true);
	Text description = new Text();
	description.setText(langPace.translationForKey(DESCRIPTION, pinType));
	getInputInfoUnits().add(description);

	PasswordField pinInputField = new PasswordField(PIN_FIELD);
	pinInputField.setDescription(pinType);
	pinInputField.setMinLength(paceMarker.getMinLength());
	pinInputField.setMaxLength(paceMarker.getMaxLength());
	getInputInfoUnits().add(pinInputField);

	Text attemptCount = new Text();
	attemptCount.setText(langPace.translationForKey("step_pin_retrycount", 3));
	attemptCount.setID(PIN_ATTEMPTS_ID);
	getInputInfoUnits().add(attemptCount);

	Text notice = new Text();
	notice.setText(langEac.translationForKey(NOTICE, pinType));
	getInputInfoUnits().add(notice);
    }

    private void addTerminalElements() {
	setInstantReturn(true);
	Text description = new Text();
	description.setText(langPace.translationForKey(DESCRIPTION_NATIVE, pinType));
	getInputInfoUnits().add(description);

	Text notice = new Text();
	notice.setText(langEac.translationForKey(NOTICE, pinType));
	getInputInfoUnits().add(notice);

	Text attemptCount = new Text();
	attemptCount.setText(langPace.translationForKey("step_pin_retrycount", 3));
	attemptCount.setID(PIN_ATTEMPTS_ID);
	getInputInfoUnits().add(attemptCount);
    }

    protected void addCANEntry() {
	PasswordField canField = new PasswordField(CAN_FIELD);
	canField.setDescription(langPace.translationForKey("can"));
	canField.setMaxLength(6);
	canField.setMinLength(6);
	getInputInfoUnits().add(canField);

	Text canNotice = new Text();
	canNotice.setText(langEac.translationForKey("eac_can_notice"));
	canNotice.setID(CAN_NOTICE_ID);
	getInputInfoUnits().add(canNotice);
    }

    protected void addNativeCANNotice() {
	Text canNotice = new Text();
	canNotice.setText(langEac.translationForKey("eac_can_notice_native"));
	canNotice.setID(CAN_NOTICE_ID);
	getInputInfoUnits().add(canNotice);
    }

    protected void updateAttemptsDisplay(int newValue) {
	for (InputInfoUnit unit : getInputInfoUnits()) {
	    if (unit.getID().equals(PIN_ATTEMPTS_ID)) {
		Text text = (Text) unit;
		text.setText(langPace.translationForKey("step_pin_retrycount", newValue));
	    }
	}
    }

}
