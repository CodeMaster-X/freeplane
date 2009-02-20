/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is created by Dimitry Polivaev in 2008.
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
package org.freeplane.features.common.nodestyle;

import java.awt.Color;
import java.awt.Font;

import org.freeplane.core.controller.Controller;
import org.freeplane.core.enums.ResourceControllerProperties;
import org.freeplane.core.extension.ExtensionContainer;
import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.io.WriteManager;
import org.freeplane.core.io.xml.TreeXmlReader;
import org.freeplane.core.modecontroller.CombinedPropertyChain;
import org.freeplane.core.modecontroller.ExclusivePropertyChain;
import org.freeplane.core.modecontroller.IPropertyGetter;
import org.freeplane.core.modecontroller.MapController;
import org.freeplane.core.modecontroller.ModeController;
import org.freeplane.core.model.NodeModel;
import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;

/**
 * @author Dimitry Polivaev
 */
public class NodeStyleController implements IExtension {
	public static Color standardNodeTextColor;

	public static <T extends ExtensionContainer> NodeStyleController getController(final T modeController) {
		return (NodeStyleController) modeController.getExtension(NodeStyleController.class);
	}

	public static void install(final ExtensionContainer modeController, final NodeStyleController styleController) {
		modeController.putExtension(NodeStyleController.class, styleController);
	}

	final private ExclusivePropertyChain<Color, NodeModel> backgroundColorHandlers;
	final private Controller controller;
	final private CombinedPropertyChain<Font, NodeModel> fontHandlers;
	final private ModeController modeController;
	final private ExclusivePropertyChain<String, NodeModel> shapeHandlers;
	final private ExclusivePropertyChain<Color, NodeModel> textColorHandlers;

	public NodeStyleController(final ModeController modeController) {
		this.modeController = modeController;
		controller = modeController.getController();
		fontHandlers = new CombinedPropertyChain<Font, NodeModel>();
		textColorHandlers = new ExclusivePropertyChain<Color, NodeModel>();
		backgroundColorHandlers = new ExclusivePropertyChain<Color, NodeModel>();
		shapeHandlers = new ExclusivePropertyChain<String, NodeModel>();
		addFontGetter(CombinedPropertyChain.NODE, new IPropertyGetter<Font, NodeModel>() {
			public Font getProperty(final NodeModel node, final Font font) {
				final NodeStyleModel nodeStyleModel = NodeStyleModel.getModel(node);
				if (nodeStyleModel == null) {
					return font;
				}
				String family = nodeStyleModel.getFontFamilyName();
				Integer size = nodeStyleModel.getFontSize();
				Boolean bold = nodeStyleModel.isBold();
				Boolean italic = nodeStyleModel.isItalic();
				if (family == null && size == null && bold == null && italic == null) {
					return font;
				}
				if (family == null) {
					family = font.getFamily();
				}
				if (size == null) {
					size = font.getSize();
				}
				if (bold == null) {
					bold = font.isBold();
				}
				if (italic == null) {
					italic = font.isItalic();
				}
				int style = 0;
				if (bold) {
					style += Font.BOLD;
				}
				if (italic) {
					style += Font.ITALIC;
				}
				return new Font(family, style, size);
			}
		});
		addFontGetter(CombinedPropertyChain.DEFAULT, new IPropertyGetter<Font, NodeModel>() {
			public Font getProperty(final NodeModel node, final Font currentValue) {
				return ResourceController.getResourceController().getDefaultFont();
			}
		});
		addColorGetter(ExclusivePropertyChain.NODE, new IPropertyGetter<Color, NodeModel>() {
			public Color getProperty(final NodeModel node, final Color currentValue) {
				return NodeStyleModel.getColor(node);
			}
		});
		addColorGetter(ExclusivePropertyChain.DEFAULT, new IPropertyGetter<Color, NodeModel>() {
			public Color getProperty(final NodeModel node, final Color currentValue) {
				return standardNodeTextColor;
			}
		});
		addBackgroundColorGetter(ExclusivePropertyChain.NODE, new IPropertyGetter<Color, NodeModel>() {
			public Color getProperty(final NodeModel node, final Color currentValue) {
				return NodeStyleModel.getBackgroundColor(node);
			}
		});
		addShapeGetter(ExclusivePropertyChain.NODE, new IPropertyGetter<String, NodeModel>() {
			public String getProperty(final NodeModel node, final String currentValue) {
				return getShape(node);
			}

			private String getShape(final NodeModel node) {
				String returnedString = NodeStyleModel.getShape(node);
				if (NodeStyleModel.getShape(node) == null) {
					if (node.isRoot()) {
						returnedString = ResourceController.getResourceController().getProperty(
						    ResourceControllerProperties.RESOURCES_ROOT_NODE_SHAPE);
					}
					else {
						final String stdstyle = ResourceController.getResourceController().getProperty(
						    ResourceControllerProperties.RESOURCES_NODE_SHAPE);
						if (stdstyle.equals(NodeStyleModel.SHAPE_AS_PARENT)) {
							returnedString = getShape(node.getParentNode());
						}
						else {
							returnedString = stdstyle;
						}
					}
				}
				else if (node.isRoot() && NodeStyleModel.getShape(node).equals(NodeStyleModel.SHAPE_AS_PARENT)) {
					returnedString = ResourceController.getResourceController().getProperty(
					    ResourceControllerProperties.RESOURCES_ROOT_NODE_SHAPE);
				}
				else if (NodeStyleModel.getShape(node).equals(NodeStyleModel.SHAPE_AS_PARENT)) {
					returnedString = getShape(node.getParentNode());
				}
				if (returnedString.equals(NodeStyleModel.SHAPE_COMBINED)) {
					if (node.getModeController().getMapController().isFolded(node)) {
						return NodeStyleModel.STYLE_BUBBLE;
					}
					else {
						return NodeStyleModel.STYLE_FORK;
					}
				}
				return returnedString;
			}
		});
		final MapController mapController = modeController.getMapController();
		final ReadManager readManager = mapController.getReadManager();
		final WriteManager writeManager = mapController.getWriteManager();
		final NodeStyleBuilder styleBuilder = new NodeStyleBuilder();
		styleBuilder.registerBy(readManager, writeManager);
		if (standardNodeTextColor == null) {
			final String stdcolor = ResourceController.getResourceController().getProperty(
			    ResourceControllerProperties.RESOURCES_NODE_TEXT_COLOR);
			standardNodeTextColor = TreeXmlReader.xmlToColor(stdcolor);
			createPropertyChangeListener();
		}
	}

	public IPropertyGetter<Color, NodeModel> addBackgroundColorGetter(final Integer key,
	                                                                  final IPropertyGetter<Color, NodeModel> getter) {
		return backgroundColorHandlers.addGetter(key, getter);
	}

	public IPropertyGetter<Color, NodeModel> addColorGetter(final Integer key,
	                                                        final IPropertyGetter<Color, NodeModel> getter) {
		return textColorHandlers.addGetter(key, getter);
	}

	public IPropertyGetter<Font, NodeModel> addFontGetter(final Integer key,
	                                                      final IPropertyGetter<Font, NodeModel> getter) {
		return fontHandlers.addGetter(key, getter);
	}

	public IPropertyGetter<String, NodeModel> addShapeGetter(final Integer key,
	                                                         final IPropertyGetter<String, NodeModel> getter) {
		return shapeHandlers.addGetter(key, getter);
	}

	private void createPropertyChangeListener() {
		final IFreeplanePropertyListener propertyChangeListener = new IFreeplanePropertyListener() {
			public void propertyChanged(final String propertyName, final String newValue, final String oldValue) {
				if (propertyName.equals(ResourceControllerProperties.RESOURCES_NODE_TEXT_COLOR)) {
					standardNodeTextColor = TreeXmlReader.xmlToColor(newValue);
					controller.getViewController().updateView();
				}
			}
		};
		ResourceController.getResourceController().addPropertyChangeListener(propertyChangeListener);
	}

	public Color getBackgroundColor(final NodeModel node) {
		return backgroundColorHandlers.getProperty(node);
	}

	public Color getColor(final NodeModel node) {
		return textColorHandlers.getProperty(node);
	}

	public Font getFont(final NodeModel node) {
		return fontHandlers.getProperty(node);
	}

	public String getFontFamilyName(final NodeModel node) {
		final Font font = getFont(node);
		return font.getFamily();
	}

	public int getFontSize(final NodeModel node) {
		final Font font = getFont(node);
		return font.getSize();
	}

	public ModeController getModeController() {
		return modeController;
	}

	public String getShape(final NodeModel node) {
		return shapeHandlers.getProperty(node);
	}

	public boolean isBold(final NodeModel node) {
		return getFont(node).isBold();
	}

	public boolean isItalic(final NodeModel node) {
		return getFont(node).isItalic();
	}

	public IPropertyGetter<Color, NodeModel> removeBackgroundColorGetter(final Integer key) {
		return backgroundColorHandlers.removeGetter(key);
	}

	public IPropertyGetter<Color, NodeModel> removeColorGetter(final Integer key) {
		return textColorHandlers.removeGetter(key);
	}

	public IPropertyGetter<Font, NodeModel> removeFontGetter(final Integer key) {
		return fontHandlers.removeGetter(key);
	}

	public IPropertyGetter<String, NodeModel> removeShapeGetter(final Integer key) {
		return shapeHandlers.removeGetter(key);
	}
}
