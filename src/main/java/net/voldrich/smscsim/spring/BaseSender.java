package net.voldrich.smscsim.spring;

import javax.annotation.PostConstruct;

import net.voldrich.smscsim.spring.auto.*;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.pdu.PduRequest;

public class BaseSender {

	protected static final Logger logger = LoggerFactory.getLogger(BaseSender.class);

	@Autowired
	private SmppSessionManager sessionManager;
	@Autowired
	private DelayedRequestSenderFactory delayedRequestSenderFactory;

	private long sendTimoutMilis = 1000;

	private DelayedRequestSenderImpl deliverSender;

	@PostConstruct
	public void init() throws Exception {
		deliverSender = delayedRequestSenderFactory.getNewDeliverSender(this.getClass().getSimpleName());
	}

	protected void send(PduRequest pdu) throws Exception {
		SmppServerSession session = sessionManager.getNextServerSession();
		if (session != null && session.isBound()) {
			session.sendRequestPdu(pdu, 1000, false);
		} else {
			logger.info("Session does not exist or is not bound {}. Deliver not sent {}", session, pdu);
		}
	}

	public SmppSessionManager getSessionManager() {
		return sessionManager;
	}

	public void setSessionManager(SmppSessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	public long getSendTimoutMilis() {
		return sendTimoutMilis;
	}

	public void setSendTimoutMilis(long sendTimoutMilis) {
		this.sendTimoutMilis = sendTimoutMilis;
	}

	public DelayedRequestSenderImpl getDeliverSender() {
		return deliverSender;
	}

	public void setDeliverSender(DelayedRequestSenderImpl deliverSender) {
		this.deliverSender = deliverSender;
	}
}
