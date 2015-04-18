package net.voldrich.smscsim.spring.auto;

import java.util.*;

import javax.annotation.*;

import net.voldrich.smscsim.server.DelayedRequestSender;

import org.jboss.netty.util.internal.ConcurrentWeakKeyHashMap;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DelayedRequestSenderManager {
	private static final Logger log = LoggerFactory.getLogger(DelayedRequestSenderManager.class);
	@Autowired
	private SmppSessionManager sessionManager;
	private int queuesLimit = 100_000;

	private Set<DelayedRequestSender> delayedRequestSenders = Collections.newSetFromMap(
			new ConcurrentWeakKeyHashMap<DelayedRequestSender, Boolean>());
	protected QueueMonitor queueMonitor;

	public DelayedRequestSenderImpl getNewDeliverSender(String id) {
		DelayedRequestSenderImpl delayedRequestSender = new DelayedRequestSenderImpl(sessionManager);
		delayedRequestSender.start(id);
		delayedRequestSenders.add(delayedRequestSender);
		queueMonitor.applyStatus(delayedRequestSender);
		return delayedRequestSender;
	}

	@PostConstruct
	public void init() {
		try {
			long maxMemory = Runtime.getRuntime().maxMemory();
			queuesLimit = Math.max((int) ((maxMemory - 50_000_000) / 400), queuesLimit);//best guess
			log.info("max memory {}k, using queueLimit={}k", maxMemory / 1000, queuesLimit / 1000);
		} catch (Exception e) {
			log.warn("using queueLimit=" + queuesLimit, e);
		}
		queueMonitor = new QueueMonitor(delayedRequestSenders, queuesLimit);
		Thread monitorThread = new Thread(queueMonitor,
				"DelayedRequestSenderManagerMonitor");
		monitorThread.setDaemon(true);
		monitorThread.start();
	}

	private static class QueueMonitor implements Runnable {
		private static final Logger log = LoggerFactory.getLogger(QueueMonitor.class);
		boolean queueBlocked;
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
						notifySenders();
					} else if (queueBlocked && totalQueued < queuesLimit / 10) {
						log.info("Accepting new messages again.");
						queueBlocked = false;
						notifySenders();
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

		private void notifySenders() {
			for (DelayedRequestSender delayedRequestSender : delayedRequestSenders) {
				delayedRequestSender.setDiscardNewDeliveries(queueBlocked);
			}
		}

		public void applyStatus(DelayedRequestSenderImpl delayedRequestSender) {
			delayedRequestSender.setDiscardNewDeliveries(queueBlocked);
		}
	}
}
