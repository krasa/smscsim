package net.voldrich.smscsim.spring.auto;

import java.util.Set;

import net.voldrich.smscsim.server.DelayedRequestSender;

import org.slf4j.*;

public class QueueMonitor implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(QueueMonitor.class);
	volatile boolean queueBlocked;
	private Set<DelayedRequestSender> delayedRequestSenders;
	private int queuesLimit;

	public QueueMonitor(Set<DelayedRequestSender> delayedRequestSenders, int queuesLimit) {
		this.delayedRequestSenders = delayedRequestSenders;
		this.queuesLimit = queuesLimit;
	}

	@Override
	public void run() {
		try {
			while (true) {
				int totalQueued = 0;
				for (DelayedRequestSender delayedRequestSender : delayedRequestSenders) {
					totalQueued += delayedRequestSender.queueSize();
				}
				if (!queueBlocked && totalQueued > queuesLimit) {
					log.info("Discarding new messages({})", totalQueued);
					queueBlocked = true;
					applyStatus();
				} else if (queueBlocked && totalQueued < (queuesLimit / 2)) {
					log.info("Accepting new messages again.");
					queueBlocked = false;
					applyStatus();
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		} finally {
			log.info("monitoring stopped");
		}
	}

	private void applyStatus() {
		for (DelayedRequestSender delayedRequestSender : delayedRequestSenders) {
			applyStatus(delayedRequestSender);
		}
	}

	public void applyStatus(DelayedRequestSender delayedRequestSender) {
		delayedRequestSender.setDiscardNewDeliveries(queueBlocked);
	}
}
