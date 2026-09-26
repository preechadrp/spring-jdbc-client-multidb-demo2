package com.mycom.springjdbcclientmultidbdemo2.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class Database1Config {
	//======== datasource1 =========//

	@Primary
	@Bean(name = "dataSourceDb1")
	@ConfigurationProperties(prefix = "custom-config.datasource1")
	HikariDataSource dataSourceDb() {
		return new HikariDataSource();
	}

	@Primary
	@Bean(name = "jdbcClientDb1")
	JdbcClient jdbcClientDb(@Qualifier("dataSourceDb1") HikariDataSource ds) {
		log.info("Init jdbcClientDb1");
		return JdbcClient.create(ds);
	}

	// ======== transaction manager db1 =========
	@Primary
	@Bean(name = "transactionManagerDb1")
	PlatformTransactionManager transactionManagerDb(
			@Qualifier("dataSourceDb1") HikariDataSource ds) {

		return new DataSourceTransactionManager(ds);
	}
}
