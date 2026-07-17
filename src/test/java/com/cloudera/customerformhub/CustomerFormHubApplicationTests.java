package com.cloudera.customerformhub;

import com.cloudera.customerformhub.config.DataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class CustomerFormHubApplicationTests {

	@MockitoBean
	private DataLoader dataLoader;

	@Test
	void contextLoads() {
	}

}
