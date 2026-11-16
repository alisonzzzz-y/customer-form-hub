package com.cloudera.customerformhub;

import com.cloudera.customerformhub.config.DataLoader;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class CustomerFormHubApplicationTests {

	@MockitoBean
	private DataLoader dataLoader;

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void startupWaitsForASleepingDatabase() {
		// The hosted MySQL drops connections while it wakes up. Without this
		// wait the app tries once and exits with status 1.
		assertThat(dataSource).isInstanceOf(HikariDataSource.class);
		assertThat(((HikariDataSource) dataSource).getInitializationFailTimeout()).isEqualTo(60_000L);
	}

}
