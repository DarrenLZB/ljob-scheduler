package cn.ljob.example;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// @PropertySource(value = "file:/data/config/ljob-scheduler-spring5-example/application.properties", ignoreResourceNotFound = true, encoding = "UTF-8")
public class Application {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(Application.class, args);
	}
}
