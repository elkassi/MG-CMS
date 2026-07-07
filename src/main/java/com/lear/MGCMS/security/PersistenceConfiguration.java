package com.lear.MGCMS.security;

import java.util.HashMap;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
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
@EnableJpaRepositories(
    basePackages = "com.lear.MGCMS.repositories", 
    entityManagerFactoryRef = "entityManagerFactory"
    //transactionManagerRef = "userTransactionManager"
)
public class PersistenceConfiguration {

	@Primary
	  @Bean(name = "dataSource")
	  @ConfigurationProperties(prefix = "spring.datasource")
	  public DataSource dataSource() {
	    return DataSourceBuilder.create().build();
	  }

	  @Primary
	  @Bean(name = "entityManagerFactory")
	  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
	      EntityManagerFactoryBuilder builder, @Qualifier("dataSource") DataSource dataSource) {

		  final LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		  em.setDataSource(dataSource);
		  em.setPackagesToScan("com.lear.MGCMS.domain");

		  final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		  em.setJpaVendorAdapter(vendorAdapter);
		  final HashMap<String, Object> properties = new HashMap<String, Object>();
		  properties.put("hibernate.hbm2ddl.auto", "none");//none , update
		  properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServer2012Dialect");

		  properties.put("hibernate.jdbc.batch_size", "1000");
		  properties.put("hibernate.order_inserts", "true");
		  properties.put("hibernate.order_updates", "true");
		  properties.put("hibernate.jdbc.batch_versioned_data", "true");


		  em.setJpaPropertyMap(properties);
		  em.setPersistenceUnitName("MGCMS");
		  return em;

	    /*return builder
	    		.dataSource(dataSource)
	    		.packages("com.lear.MGCMS.domain")
	    		.persistenceUnit("MGCMS")
	    		.build();*/
	  }

	  @Primary
	  @Bean(name = "transactionManager")
	  public PlatformTransactionManager transactionManager(
	      @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
	    return new JpaTransactionManager(entityManagerFactory);
	  }
	
}
