package net.voldrich.smscsim.server;

import java.lang.ref.WeakReference;

import net.voldrich.smscsim.spring.*;
import net.voldrich.smscsim.spring.auto.*;

import org.slf4j.*;

import com.cloudhopper.smpp.*;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.*;

public class SmscSmppSessionHandler extends DefaultSmppSessionHandler {

	private static final Logger logger = LoggerFactory.getLogger(SmscSmppSessionHandler.class);

	private WeakReference<SmppSession> sessionRef;

	private DelayedRequestSenderImpl deliverSender;

	private ResponseMessageIdGenerator messageIdGenerator;

	private DeliveryReceiptScheduler deliveryReceiptScheduler;

	public SmscSmppSessionHandler(SmppServerSession session, SmscGlobalConfiguration config) {
		this.sessionRef = new WeakReference<SmppSession>(session);
		this.deliverSender = config.getDeliverSender(session.getConfiguration().getSystemId());
		this.messageIdGenerator = config.getMessageIdGenerator();
		this.deliveryReceiptScheduler = config.getDeliveryReceiptScheduler();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public PduResponse firePduRequestReceived(PduRequest pduRequest) {
		SmppSession session = sessionRef.get();

		if (pduRequest instanceof SubmitSm) {
			SubmitSm submitSm = (SubmitSm) pduRequest;
			SubmitSmResp submitSmResp = submitSm.createResponse();
			long messageId = messageIdGenerator.getNextMessageId();
			submitSmResp.setMessageId(FormatUtils.formatAsHex(messageId));
			try {
				// We can not wait in this thread!!
				// It would block handling of other messages and performance would drop drastically!!
				// create and enqueue delivery receipt
				if (submitSm.getRegisteredDelivery() > 0 && deliverSender != null) {
					DeliveryReceiptRecord record = new DeliveryReceiptRecord(session, submitSm, messageId);
					record.setDeliverTime(deliveryReceiptScheduler.getDeliveryTimeMillis());
					deliverSender.scheduleDelivery(record);
				}
			} catch (Exception e) {
				logger.error("Error when handling submit", e);
			}

			// submitSmResp.setCommandStatus(SmppConstants.STATUS_X_T_APPN);
			return submitSmResp;
			// return null;
		} else if (pduRequest instanceof Unbind) {
			return pduRequest.createResponse();
		}

		return pduRequest.createResponse();
	}

	@Override
	public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
		if (pduAsyncResponse.getResponse().getCommandStatus() != SmppConstants.STATUS_OK) {
			// TODO
			// error, resend the request again?
			// pduAsyncResponse.getRequest().setReferenceObject(value)
		}
	}

	public void destroy() {
		deliverSender.scheduleStop();
	}
}
