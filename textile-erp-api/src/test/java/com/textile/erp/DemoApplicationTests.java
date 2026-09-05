package com.textile.erp;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
		assertThat(dataSource).isNotNull();
	}

	@Test
	void databaseConnectionIsHealthy() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.isValid(2)).isTrue();
			assertThat(connection.getCatalog()).isEqualTo("texforge");
		}
	}

}
