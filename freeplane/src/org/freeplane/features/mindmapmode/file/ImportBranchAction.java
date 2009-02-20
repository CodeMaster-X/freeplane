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
package org.freeplane.features.mindmapmode.file;

import java.awt.event.ActionEvent;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.freeplane.core.actions.IFreeplaneAction;
import org.freeplane.core.controller.Controller;
import org.freeplane.core.extension.ControllerUtil;
import org.freeplane.core.model.MapModel;
import org.freeplane.core.model.NodeModel;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.url.UrlManager;
import org.freeplane.features.common.clipboard.ClipboardController;
import org.freeplane.features.mindmapmode.MMapController;
import org.freeplane.features.mindmapmode.clipboard.MClipboardController;

class ImportBranchAction extends AFreeplaneAction implements IFreeplaneAction {
    private static final String NAME = "importBranch";
	private static final long serialVersionUID = 7546992261152336279L;

	public ImportBranchAction(final Controller controller) {
		super(controller, "import_branch");
	}

	public void actionPerformed(final ActionEvent e) {
		final NodeModel parent = getModeController().getMapController().getSelectedNode();
		if (parent == null) {
			return;
		}
		final JFileChooser chooser = new JFileChooser();
		final FileFilter fileFilter = ((MFileManager) UrlManager.getController(getModeController())).getFileFilter();
		if (fileFilter != null) {
			chooser.addChoosableFileFilter(fileFilter);
		}
		final int returnVal = chooser.showOpenDialog(getController().getViewController().getContentPane());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				final MapModel map = parent.getMap();
				final NodeModel node = ((MMapController) getModeController().getMapController()).loadTree(map, chooser
				    .getSelectedFile());
				((MClipboardController) getController().getModeController().getClipboardController()).paste(
				    node, parent);
			}
			catch (final Exception ex) {
				UrlManager.getController(getModeController()).handleLoadingException(ex);
			}
		}
	}

	public String getName() {
	    return NAME;
    }
}
