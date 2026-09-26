package com.mycom.springjdbcclientmultidbdemo2.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class Database2Config {
	//======== datasource2 =========//

	@Bean(name = "dataSourceDb2")
	@ConfigurationProperties(prefix = "custom-config.datasource2")
	HikariDataSource dataSourceDb() {
		return new HikariDataSource();
	}

	@Bean(name = "jdbcClientDb2")
	JdbcClient jdbcClientDb(@Qualifier("dataSourceDb2") HikariDataSource ds) {
		log.info("Init jdbcClientDb2");
		return JdbcClient.create(ds);
	}

	// ======== transaction manager db2 =========
	@Bean(name = "transactionManagerDb2")
	PlatformTransactionManager transactionManagerDb(
			@Qualifier("dataSourceDb2") HikariDataSource ds) {

		return new DataSourceTransactionManager(ds);
	}
}
