package com.lear.MGCMS.utils;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.IdentifierGenerator;

public class CustomSequenceGenerator implements IdentifierGenerator {

    private static final String DATE_FORMAT = "ddMMyyHHmm";
    private static final String SEQUENCE_FORMAT = "%02d";
    private int sequence = 0;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        String sequenceNumber = String.format(SEQUENCE_FORMAT, ++sequence);
        return date + sequenceNumber;
    }
}
