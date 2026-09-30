package com.openomada.plugin.tr069.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the React Admin GUI for TR-069 management in Omada Controller.
 * Forwards requests from /tr069-admin to /tr069-admin/index.html.
 */
@Controller("tr069AdminWebController")
public class Tr069AdminWebController {

    @GetMapping(value = {"/tr069-admin", "/tr069-admin/"})
    public String adminIndex() {
        return "redirect:/tr069-admin/index.html";
    }
}
