package com.lear.MGCMS.security;

import java.util.HashMap;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.lear.ctc.repositories", entityManagerFactoryRef = "ctcEntityManagerFactory", transactionManagerRef = "ctcTransactionManager")
public class PersistenceCTCConfiguration {

	@Bean(name = "ctcDataSource")
	@ConfigurationProperties(prefix = "ctc.datasource")
	public DataSource dataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "ctcEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean ctcEntityManagerFactory(
			EntityManagerFactoryBuilder builder, @Qualifier("ctcDataSource") DataSource dataSource) {

		final LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource);
		em.setPackagesToScan("com.lear.ctc.domain");

		final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);
		final HashMap<String, Object> properties = new HashMap<String, Object>();
		properties.put("hibernate.hbm2ddl.auto", "none");// none , update
		properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServer2012Dialect");
		em.setJpaPropertyMap(properties);
		em.setPersistenceUnitName("ctc");
		return em;

		/*
		 * return builder.dataSource(dataSource).packages("com.lear.ctc.domain").
		 * persistenceUnit("ctc")
		 * .build();
		 */
	}

	@Bean(name = "ctcTransactionManager")
	public PlatformTransactionManager ctcTransactionManager(
			@Qualifier("ctcEntityManagerFactory") EntityManagerFactory ctcEntityManagerFactory) {
		return new JpaTransactionManager(ctcEntityManagerFactory);
	}

}
