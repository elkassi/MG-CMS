package com.lear.MGCMS.services;

import java.io.File;
import java.util.List;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.lear.MGCMS.security.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service("emailService")
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	@Autowired
    private JavaMailSender mailSender;
      
    //@Autowired
    //private SimpleMailMessage preConfiguredMessage;
  
    /**
     * This method will send compose and send the message 
     * */
    public void sendMail(String to, String subject, String body) 
    {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
  
    /**
     * This method will send a pre-configured message
     * */
    /*public void sendPreConfiguredMail(String message) 
    {
        SimpleMailMessage mailMessage = new SimpleMailMessage(preConfiguredMessage);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }*/
    
    public void sendEmailAttachment(String to,  String subject, String body) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
			mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mimeMessage.setFrom(new InternetAddress("CMS_WEB@LEAR.COM"));
            mimeMessage.setSubject(subject);
            helper.setText(body, true);
			mailSender.send(mimeMessage);
			System.out.println("Email sending(" +to+")" );
		} catch (Exception e) {
			log.error("EmailService send failed", e);
		}
	}
    public void sendEmailAttachment(List<String> listTo,  String subject, String body) {
//		System.out.println(body);
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
			InternetAddress[] address = new InternetAddress[listTo.size()];
			for(int i =0 ; i<listTo.size(); i++) {
				address[i] = new InternetAddress(listTo.get(i));
			}
			mimeMessage.setRecipients(Message.RecipientType.TO, address);
			mimeMessage.setFrom(new InternetAddress("CMS_WEB@LEAR.COM"));
            mimeMessage.setSubject(subject);
            helper.setText(body, true);
//             System.out.println(body);
			mailSender.send(mimeMessage);
		} catch (Exception e) {
			log.error("EmailService send failed", e);
		}
	}

	public void sendEmailAttachment(List<String> listTo, List<String> listToCC,  String subject, String body, List<File> attachments) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
			InternetAddress[] address = new InternetAddress[listTo.size()];
			for(int i =0 ; i<listTo.size(); i++) {
				address[i] = new InternetAddress(listTo.get(i));
			}
			InternetAddress[] addressCC = new InternetAddress[listToCC.size()];
			for(int i =0 ; i<listToCC.size(); i++) {
				addressCC[i] = new InternetAddress(listToCC.get(i));
			}


			mimeMessage.setRecipients(Message.RecipientType.TO, address);
			mimeMessage.setRecipients(Message.RecipientType.CC, addressCC);
			mimeMessage.setFrom(new InternetAddress("CMS_WEB@LEAR.COM"));
			mimeMessage.setSubject(subject);
			helper.setText(body, true);

			for (File file : attachments) {
				helper.addAttachment(file.getName(), file);
			}

//             System.out.println(body);
			mailSender.send(mimeMessage);
		} catch (Exception e) {
			Constants.writeLogs(e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				Constants.writeLogs(element.toString());
			}
		}
	}


}
