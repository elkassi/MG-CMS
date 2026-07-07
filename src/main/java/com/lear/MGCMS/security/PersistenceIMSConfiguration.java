package com.lear.MGCMS.security;

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

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.lear.ims.repositories", entityManagerFactoryRef = "imsEntityManagerFactory", transactionManagerRef = "imsTransactionManager")
public class PersistenceIMSConfiguration {

    @Bean(name = "imsDataSource")
    @ConfigurationProperties(prefix = "ims.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "imsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean imsEntityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("imsDataSource") DataSource dataSource) {

        final LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.lear.ims.domain");

        final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        final HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("hibernate.hbm2ddl.auto", "none");// none , update
        properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServer2012Dialect");
        em.setJpaPropertyMap(properties);
        em.setPersistenceUnitName("ims");
        return em;

        /*
         * return builder.dataSource(dataSource).packages("com.lear.ims.domain").
         * persistenceUnit("ims")
         * .build();
         */
    }

    @Bean(name = "imsTransactionManager")
    public PlatformTransactionManager imsTransactionManager(
            @Qualifier("imsEntityManagerFactory") EntityManagerFactory imsEntityManagerFactory) {
        return new JpaTransactionManager(imsEntityManagerFactory);
    }

}
