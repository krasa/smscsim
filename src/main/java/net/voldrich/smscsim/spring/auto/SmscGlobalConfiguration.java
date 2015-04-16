package net.voldrich.smscsim.spring.auto;

import net.voldrich.smscsim.spring.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SmscGlobalConfiguration {

	@Autowired
	private DelayedRequestSenderFactory deliverSenderPool;

	@Autowired
	private ResponseMessageIdGenerator messageIdGenerator;

	@Autowired
	private SmppSessionManager sessionManager;

	@Autowired
	private DeliveryReceiptScheduler deliveryReceiptScheduler;

	public DelayedRequestSenderImpl getDeliverSender(String systemId) {
		return deliverSenderPool.getNewDeliverSender(systemId);
	}

	public void setDeliverSenderPool(DelayedRequestSenderFactory deliverSenderPool) {
		this.deliverSenderPool = deliverSenderPool;
	}

	public ResponseMessageIdGenerator getMessageIdGenerator() {
		return messageIdGenerator;
	}

	public void setMessageIdGenerator(ResponseMessageIdGenerator messageIdGenerator) {
		this.messageIdGenerator = messageIdGenerator;
	}

	public SmppSessionManager getSessionManager() {
		return sessionManager;
	}

	public void setSessionManager(SmppSessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	public DeliveryReceiptScheduler getDeliveryReceiptScheduler() {
		return deliveryReceiptScheduler;
	}

	public void setDeliveryReceiptScheduler(DeliveryReceiptScheduler deliveryReceiptScheduler) {
		this.deliveryReceiptScheduler = deliveryReceiptScheduler;
	}
}
