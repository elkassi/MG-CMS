package com.lear.MGCMS.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

	@RequestMapping({"/",  "/{file:(?!bundle\\.js|api)[a-zA-Z0-9_-]*}",  "/{file:(?!bundle\\.js|api)[a-zA-Z0-9_-]*}/**" })
    public String landingPage(HttpServletRequest request) { //[a-zA-Z0-9_-]
		if(request.getRequestURI().endsWith(".jpg") || request.getRequestURI().endsWith(".png") || request.getRequestURI().endsWith(".js")) {
			String[] req = request.getRequestURI().split("/");
			return "forward:" + "/" + req[req.length - 1];
		}
        return "index";
    }
	
	
}
