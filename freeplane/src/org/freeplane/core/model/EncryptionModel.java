/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.core.model;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import org.freeplane.core.enums.ResourceControllerProperties;
import org.freeplane.core.extension.IExtension;
import org.freeplane.core.modecontroller.IEncrypter;
import org.freeplane.core.modecontroller.MapController;
import org.freeplane.core.util.LogTool;

public class EncryptionModel implements IExtension {
	private static ImageIcon decryptedIcon;
	private static ImageIcon encryptedIcon;
	private static Logger logger;

	public static EncryptionModel getModel(final NodeModel node) {
		return (EncryptionModel) node.getExtension(EncryptionModel.class);
	}

	private String encryptedContent;
	private boolean isAccessible = true;
	/**
	 * is only set to false by the load mechanism. If the node is generated or
	 * it is decrypted once, this is always true.
	 */
	private boolean isDecrypted = true;
	IEncrypter mEncrypter;
	/**
	 * password have to be stored in a StringBuffer as Strings cannot be deleted
	 * or overwritten.
	 */
	final private NodeModel node;

	public EncryptionModel(final NodeModel node) {
		this.node = node;
		encryptedContent = null;
		setAccessible(true);
		isDecrypted = true;
		init(node);
	}

	/**
	 * @param encryptedContent
	 */
	public EncryptionModel(final NodeModel node, final String encryptedContent) {
		this.node = node;
		this.encryptedContent = encryptedContent;
		setAccessible(false);
		isDecrypted = false;
		init(node);
	}

	/**
	 */
	public boolean checkPassword(final IEncrypter encrypter) {
		final String decryptedNode = decryptXml(encryptedContent, encrypter);
		if (decryptedNode == null || decryptedNode.equals("") || !decryptedNode.startsWith("<node ")) {
			EncryptionModel.logger.warning("Wrong password supplied (stored!=given).");
			return false;
		}
		mEncrypter = encrypter;
		return true;
	}

	/**
	 * @return true, if the password was correct.
	 */
	public boolean decrypt(final IEncrypter encrypter) {
		if (!checkPassword(encrypter)) {
			return false;
		}
		setAccessible(true);
		if (!isDecrypted) {
			try {
				final String childXml = decryptXml(encryptedContent, encrypter);
				final String[] childs = childXml.split(ResourceControllerProperties.NODESEPARATOR);
				for (int i = childs.length - 1; i >= 0; i--) {
					final String string = childs[i];
					if (string.length() == 0) {
						continue;
					}
					pasteXML(string, node);
				}
				isDecrypted = true;
			}
			catch (final Exception e) {
				LogTool.logException(e);
				setAccessible(false);
				return true;
			}
		}
		return true;
	}

	/**
	 * @return null if the password is wrong.
	 */
	private String decryptXml(final String encryptedString, final IEncrypter encrypter) {
		final String decrypted = encrypter.decrypt(encryptedString);
		return decrypted;
	}

	/**
	 */
	private String encryptXml(final StringBuffer childXml) {
		try {
			final String encrypted = mEncrypter.encrypt(childXml.toString());
			return encrypted;
		}
		catch (final Exception e) {
			LogTool.logException(e);
		}
		throw new IllegalArgumentException("Can't encrypt the node.");
	}

	/**
	 * @throws IOException
	 */
	private void generateEncryptedContent() throws IOException {
		final StringWriter sWriter = new StringWriter();
		for (final Iterator i = node.getModeController().getMapController().childrenUnfolded(node); i.hasNext();) {
			final NodeModel child = (NodeModel) i.next();
			child.getModeController().getMapController().getMapWriter().writeNodeAsXml(sWriter, child, true, true);
			if (i.hasNext()) {
				sWriter.write(ResourceControllerProperties.NODESEPARATOR);
			}
		}
		final StringBuffer childXml = sWriter.getBuffer();
		encryptedContent = encryptXml(childXml);
	}

	public String getEncryptedContent() {
		if (isDecrypted) {
			try {
				generateEncryptedContent();
			}
			catch (final Exception e) {
				LogTool.logException(e);
			}
		}
		return encryptedContent;
	}

	private void init(final NodeModel node) {
		if (EncryptionModel.logger == null) {
			EncryptionModel.logger = Logger.global;
		}
		if (EncryptionModel.encryptedIcon == null) {
			EncryptionModel.encryptedIcon = MindIcon.factory("encrypted").getIcon();
		}
		if (EncryptionModel.decryptedIcon == null) {
			EncryptionModel.decryptedIcon = MindIcon.factory("decrypted").getIcon();
		}
		updateIcon();
	}

	/**
	 * @return Returns the isAccessible (ie. if the node is decrypted
	 *         (isAccessible==true) or not).
	 */
	public boolean isAccessible() {
		return isAccessible;
	}

	/**
	 *
	 */
	public boolean isFolded() {
		if (isAccessible()) {
			return node.getModeController().getMapController().isFolded(node);
		}
		return true;
	}

	private void pasteXML(final String pasted, final NodeModel target) {
		try {
			final MapController mapController = target.getModeController().getMapController();
			final NodeModel node = mapController.getMapReader().createNodeTreeFromXml(target.getMap(),
			    new StringReader(pasted));
			mapController.insertNodeIntoWithoutUndo(node, target, target.getChildCount());
		}
		catch (final Exception ee) {
			LogTool.logException(ee);
		}
	}

	/**
	 * @param isAccessible
	 *            The isAccessible to set.
	 */
	public void setAccessible(final boolean isAccessible) {
		this.isAccessible = isAccessible;
		updateIcon();
	}

	public void setEncrypter(final IEncrypter encrypter) {
		mEncrypter = encrypter;
	}

	/*
	 * (non-Javadoc)
	 * @see freeplane.modes.MindMapNode#getIcons()
	 */
	public void updateIcon() {
		if (isAccessible()) {
			node.setStateIcon("encrypted", null);
			node.setStateIcon("decrypted", EncryptionModel.decryptedIcon);
		}
		else {
			node.setStateIcon("decrypted", null);
			node.setStateIcon("encrypted", EncryptionModel.encryptedIcon);
		}
	}
}
