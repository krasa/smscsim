package net.voldrich.smscsim.server;

import java.util.concurrent.*;

import org.slf4j.*;

/**
 * Base implementation for delivery queues.
 */
public abstract class DelayedRequestSender<T extends DelayedRecord> implements RequestSender<T> {

	private static final String DELAYED_QUEUE_HANDLER_THREAD_NAME = "DelayedQueueHandler";

	private static final Logger logger = LoggerFactory.getLogger(DelayedRequestSender.class);

	protected final DelayQueue<T> deliveryReceiptQueue;

	private Thread deliveryReceiptQueueHandlerThread;

	private volatile boolean stop;

	public DelayedRequestSender() {
		deliveryReceiptQueue = new DelayQueue<T>();
	}

	protected abstract void handleDelayedRecord(T delayedRecord) throws Exception;

	@Override
	public void scheduleDelivery(T record) {
		deliveryReceiptQueue.offer(record);
	}

	@Override
	public void scheduleDelivery(T record, int minDelayMs, int randomDeltaMs) {
		record.setDeliverTime(minDelayMs, randomDeltaMs);
		deliveryReceiptQueue.offer(record);
	}

	public void start(String id) {
		if (deliveryReceiptQueueHandlerThread == null) {
			deliveryReceiptQueueHandlerThread = new Thread(new QueueHandlerImpl(), DELAYED_QUEUE_HANDLER_THREAD_NAME
					+ "-" + id);
			deliveryReceiptQueueHandlerThread.setDaemon(true);
			deliveryReceiptQueueHandlerThread.start();
		}
	}

	public void scheduleStop() {
		stop = true;
		if (deliveryReceiptQueue.size() == 0) {
			stop();
		}
	}

	public void stop() {
		if (deliveryReceiptQueueHandlerThread != null) {
			deliveryReceiptQueueHandlerThread.interrupt();
		}
	}

	public Thread startThreadWhichTerminatesWhenQueueEmpty(String systemId) {
		Thread thread = new Thread(new QueueHandlerUntillEmptyImpl(), DELAYED_QUEUE_HANDLER_THREAD_NAME + "-"
				+ systemId);
		thread.start();
		return thread;
	}

	/**
	 * Implementation which terminates when queue is empty
	 */
	private final class QueueHandlerUntillEmptyImpl implements Runnable {
		@Override
		public void run() {
			try {
				for (; ; ) {
					if (deliveryReceiptQueue.size() == 0) {
						logger.info("Queue empty, terminating " + Thread.currentThread().getName());
						return;
					}
					T delayedRecord = deliveryReceiptQueue.take();
					handleDelayedRecord(delayedRecord);
				}
			} catch (InterruptedException ex) {
				logger.info("Received interrupt, terminating " + Thread.currentThread().getName());
				return;
			} catch (Exception ex) {
				logger.error("Error when handling delayed record", ex);
			} finally {
				deliveryReceiptQueueHandlerThread = null;
			}
		}
	}

	/**
	 * Runs indefinitely until an interrupt is caught.
	 */
	private final class QueueHandlerImpl implements Runnable {
		@Override
		public void run() {
			try {
				for (; ; ) {
					try {
						if (stop && deliveryReceiptQueue.size() == 0) {
							logger.info("Queue empty, terminating " + Thread.currentThread().getName());
							return;
						}
						T delayedRecord = deliveryReceiptQueue.poll(5, TimeUnit.SECONDS);
						if (delayedRecord != null) {
							handleDelayedRecord(delayedRecord);
						}
					} catch (InterruptedException ex) {
						throw ex;
					} catch (Exception ex) {
						logger.error("Error when handling delayed record", ex);
					}
				}
			} catch (InterruptedException ex) {
				logger.info("Received interrupt, terminating " + Thread.currentThread().getName());
				return;
			} finally {
				deliveryReceiptQueueHandlerThread = null;
			}
		}
	}

}
