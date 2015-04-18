package net.voldrich.smscsim.spring.auto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DelayedRequestSenderFactory {
	@Autowired
	private SmppSessionManager sessionManager;

	public DelayedRequestSenderImpl getNewDeliverSender(String id) {
		DelayedRequestSenderImpl delayedRequestSender = new DelayedRequestSenderImpl(sessionManager);
		delayedRequestSender.start(id);
		return delayedRequestSender;
	}
}
