package com.example.cometdspring;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.cometd.annotation.server.AnnotationCometDServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletContextInitializer;

@SpringBootApplication
public class CometdSpringApplication implements ServletContextInitializer {

	public static void main(String[] args) {
		SpringApplication.run(CometdSpringApplication.class, args);
	}

	@Override
	public void onStartup(ServletContext servletContext) {
		ServletRegistration.Dynamic cometdServlet = servletContext.addServlet("cometd", AnnotationCometDServlet.class);
		String mapping = "/cometd/*";
		cometdServlet.addMapping(mapping);
		cometdServlet.setAsyncSupported(true);
		cometdServlet.setLoadOnStartup(1);
		cometdServlet.setInitParameter("ws.cometdURLMapping", "/cometd/*");

		ServletRegistration.Dynamic demoServlet = servletContext.addServlet("demo", CometdDemoServlet.class);
//		demoServlet.addMapping("/demo");
		demoServlet.setAsyncSupported(true);
		demoServlet.setLoadOnStartup(2);
	}

}
