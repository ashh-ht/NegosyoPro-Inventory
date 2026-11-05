package com.inventory.negosyopro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// The app talks to MySQL through plain JDBC (db/dbConnection.java), so Spring must not try to build a
// DataSource or JPA setup of its own. excludeName takes strings, so this compiles whether or not the
// JPA / JDBC starters are in the pom.
@SpringBootApplication(excludeName = {
		"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
		"org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration" })
public class NegosyoproApplication {

	public static void main(String[] args) {
		SpringApplication.run(NegosyoproApplication.class, args);
	}

}
