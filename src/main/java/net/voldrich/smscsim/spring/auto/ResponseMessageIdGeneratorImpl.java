package net.voldrich.smscsim.spring.auto;

import java.util.concurrent.atomic.AtomicLong;

import net.voldrich.smscsim.spring.ResponseMessageIdGenerator;

import javax.annotation.PostConstruct;

/**
 * Handles message ID generation and formating.
 **/
public class ResponseMessageIdGeneratorImpl implements ResponseMessageIdGenerator {
	
	private AtomicLong nextMessageId;
	
	private long initialMessageIdValue;

	public ResponseMessageIdGeneratorImpl() {
		nextMessageId = new AtomicLong();		
	}
	
	@PostConstruct
	public void init() {
		ResponseMessageIdPersistence responseMessageIdPersistence = new ResponseMessageIdPersistence(this);
		responseMessageIdPersistence.startMessageIdBackupJob();
		this.nextMessageId.set(responseMessageIdPersistence.loadMessageId(nextMessageId.get()));
	}

	/* (non-Javadoc)
	 * @see net.voldrich.smscsim.server.ResponseMessageIdGenerator#getNextMessageId()
	 */
	@Override
	public long getNextMessageId() {
		return nextMessageId.incrementAndGet();				
	}

	public long getLastMessageId() {
		return nextMessageId.get();
	}

	public long getInitialMessageIdValue() {
		return initialMessageIdValue;
	}

	public void setInitialMessageIdValue(long initialMessageIdValue) {
		this.initialMessageIdValue = initialMessageIdValue;
		this.nextMessageId.set(initialMessageIdValue);
	}
	
}
