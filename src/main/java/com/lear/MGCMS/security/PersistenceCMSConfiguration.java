package com.lear.MGCMS.security;

import java.util.HashMap;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.lear.cms.repositories", entityManagerFactoryRef = "cmsEntityManagerFactory", transactionManagerRef = "cmsTransactionManager")
public class PersistenceCMSConfiguration {

	@Bean(name = "cmsDataSource")
	@ConfigurationProperties(prefix = "cms.datasource")
	public DataSource dataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "cmsEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean cmsEntityManagerFactory(
			EntityManagerFactoryBuilder builder, @Qualifier("cmsDataSource") DataSource dataSource) {

		final LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource);
		em.setPackagesToScan("com.lear.cms.domain");

		final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);
		final HashMap<String, Object> properties = new HashMap<String, Object>();
		properties.put("hibernate.hbm2ddl.auto", "none");// none , update
		properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServer2012Dialect");
		em.setJpaPropertyMap(properties);
		em.setPersistenceUnitName("cms");
		return em;

		/*
		 * return builder.dataSource(dataSource).packages("com.lear.cms.domain").
		 * persistenceUnit("cmsPLS")
		 * .build();
		 */
	}

	@Bean(name = "cmsTransactionManager")
	public PlatformTransactionManager cmsTransactionManager(
			@Qualifier("cmsEntityManagerFactory") EntityManagerFactory cmsEntityManagerFactory) {
		return new JpaTransactionManager(cmsEntityManagerFactory);
	}

}
