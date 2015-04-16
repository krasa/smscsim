package net.voldrich.smscsim.spring.auto;

import net.voldrich.smscsim.server.*;

import org.slf4j.*;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.PduRequest;

public class DelayedRequestSenderImpl extends DelayedRequestSender<DelayedRecord> {

	private static final Logger logger = LoggerFactory.getLogger(DelayedRequestSenderImpl.class);

	private SmppSessionManager sessionManager;

	private long sendTimoutMilis = 1000;

	public DelayedRequestSenderImpl(SmppSessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	@Override
	protected void handleDelayedRecord(DelayedRecord delayedRecord) throws Exception {
		SmppSession session = delayedRecord.getUsedSession(sessionManager);
		PduRequest request = delayedRecord.getRequest(sessionManager.getNextSequenceNumber());
		if (session != null && session.isBound()) {
			session.sendRequestPdu(request, sendTimoutMilis, false);
		} else {
			logger.info("Session does not exist or is not bound {}. Request not sent {}", session, request);
		}
		if (session == null) {
			logger.info("No sessions for receiving DR, clearing queue");
			deliveryReceiptQueue.clear();
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

}
