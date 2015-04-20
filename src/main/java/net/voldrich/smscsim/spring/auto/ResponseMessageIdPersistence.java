package net.voldrich.smscsim.spring.auto;

import static java.nio.file.Paths.get;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.*;
import org.springframework.core.io.FileSystemResource;

import com.google.common.base.Charsets;

public class ResponseMessageIdPersistence {
	private static final Logger log = LoggerFactory.getLogger(ResponseMessageIdPersistence.class);

	private static final FileSystemResource CURRENT = new FileSystemResource("smscsim.messageId.txt");
	private static final FileSystemResource BACKUP = new FileSystemResource("smscsim.messageId.backup");
	private static final FileSystemResource TMP = new FileSystemResource("smscsim.messageId.tmp");

	private ResponseMessageIdGeneratorImpl responseMessageIdGenerator;
	private AtomicLong lastMessageId;

	public ResponseMessageIdPersistence(ResponseMessageIdGeneratorImpl responseMessageIdGenerator) {
		this.responseMessageIdGenerator = responseMessageIdGenerator;
		lastMessageId = new AtomicLong(responseMessageIdGenerator.getLastMessageId());
	}

	public long loadMessageId(long initialMessageIdValue) {
		Long messageId;
		Long previous = loadIfExists(CURRENT);
		if (previous == null) {
			previous = loadIfExists(BACKUP);
		}

		if (previous != null) {
			messageId = Math.max(initialMessageIdValue, previous);
		} else {
			messageId = initialMessageIdValue;
		}

		log.info("MessageId = {}", messageId);
		return messageId;
	}

	public void saveMessageId() {
		long messageId = responseMessageIdGenerator.getLastMessageId();
		if (lastMessageId.getAndSet(messageId) != messageId) {
			try {
				saveTmp();
				backupCurrent();
				renameTmpToCurrent();
				deleteBackup();
			} catch (Exception e) {
				log.warn("Persisting failed", e);
			}
		}
	}

	public void startMessageIdBackupJob() {
		final Runnable backupTask = new Runnable() {

			@Override
			public void run() {
				saveMessageId();
			}
		};
		Runtime.getRuntime().addShutdownHook(new Thread(backupTask));

		Thread periodicBackup = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						return;
					}
					backupTask.run();
				}
			}
		}, "ResponseMessageIdBackup");
		periodicBackup.setDaemon(true);
		periodicBackup.start();
	}

	private Long loadIfExists(FileSystemResource file) {
		Long messageId = null;
		if (file.exists()) {
			try {
				String content = new String(Files.readAllBytes(get(file.getURI())), Charsets.UTF_8);
				messageId = Long.parseLong(content);
			} catch (Exception e1) {
				log.warn("Loading of messageId failed for " + file.getPath(), e1);
			}
		}
		return messageId;
	}

	private void saveTmp() throws IOException {
		long messageId = responseMessageIdGenerator.getLastMessageId();
		Files.write(get(TMP.getURI()), String.valueOf(messageId).getBytes(Charsets.UTF_8));
	}

	private void backupCurrent() throws IOException {
		if (CURRENT.exists()) {
			Files.move(get(CURRENT.getURI()), get(BACKUP.getURI()), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void renameTmpToCurrent() throws IOException {
		Files.move(get(TMP.getURI()), get(CURRENT.getURI()), StandardCopyOption.REPLACE_EXISTING);
	}

	private void deleteBackup() throws IOException {
		try {
			Files.delete(get(BACKUP.getURI()));
		} catch (Exception e) {
			log.warn("backup deletion failed", e);
		}
	}

}
