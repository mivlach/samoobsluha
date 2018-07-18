package cz.csas.mw.pipes;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication(scanBasePackages={
		"cz.csas.mw"})
//@ImportResource("file:/Users/cathy/sources/soappipe/src/main/webapp/WEB-INF/config/config.xml")
@ImportResource("classpath:config.xml")
public class PipeApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(PipeApplication.class, args);
	}
}
