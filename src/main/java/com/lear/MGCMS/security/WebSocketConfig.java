package com.lear.MGCMS.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig 
extends AbstractSecurityWebSocketMessageBrokerConfigurer 
implements WebSocketMessageBrokerConfigurer  {

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/api/topic", "/topic");
		config.setApplicationDestinationPrefixes("/api/ws", "/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/api/gs-guide-websocket").withSockJS();
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
	}
	
	@Override 
	protected void configureInbound(
	  MessageSecurityMetadataSourceRegistry messages) { 
	    messages
	      .simpDestMatchers("/api/**").permitAll();
	}

	 @Override
	 protected boolean sameOriginDisabled() {
	     return true;
	 }
}
