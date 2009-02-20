package org.freeplane.features.controller.help;

import org.freeplane.core.controller.Controller;

public class WebDocuAction extends OpenURLAction {

    private static final String NAME = "webDocu";

    private static final long serialVersionUID = 9103877920868471662L;

    WebDocuAction(Controller controller, String description, String url) {
	    super(controller, description, url);
    }

	@Override
    public String getName() {
	    return NAME;
    }

}
