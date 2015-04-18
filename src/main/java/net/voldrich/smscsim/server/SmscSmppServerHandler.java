package net.voldrich.smscsim.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.voldrich.smscsim.spring.auto.*;

import org.slf4j.*;

import com.cloudhopper.smpp.*;
import com.cloudhopper.smpp.pdu.*;
import com.cloudhopper.smpp.type.SmppProcessingException;

public class SmscSmppServerHandler implements SmppServerHandler {

	private static final Logger logger = LoggerFactory.getLogger(SmscSmppServerHandler.class);

	private SmppSessionManager sessionManager;

	private SmscGlobalConfiguration config;
	private Map<Long, SmscSmppSessionHandler> handlerMap = new ConcurrentHashMap<>();

	public SmscSmppServerHandler(SmscGlobalConfiguration config) {
		this.config = config;
		this.sessionManager = config.getSessionManager();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration,
			final BaseBind bindRequest) throws SmppProcessingException {
		// test name change of sessions
		// this name actually shows up as thread context....
		sessionConfiguration.setName("Application.SMPP." + sessionConfiguration.getSystemId());
	}

	@Override
	public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse)
			throws SmppProcessingException {
		logger.info("Session created: {}", sessionId);
		// need to do something it now (flag we're ready)
		// we need to create one session handler instance per session
		SmscSmppSessionHandler smppSessionHandler = new SmscSmppSessionHandler(session, config, sessionId);
		session.serverReady(smppSessionHandler);
		handlerMap.put(sessionId, smppSessionHandler);
		sessionManager.addServerSession(session);
	}

	@Override
	public void sessionDestroyed(Long sessionId, SmppServerSession session) {
		logger.info("Session destroyed: {}", sessionId);
		// print out final stats
		if (session.hasCounters()) {
			logger.info(" final session rx-submitSM: {}", session.getCounters().getRxSubmitSM());
		}

		sessionManager.removeServerSession(sessionId, session);
		SmscSmppSessionHandler smscSmppSessionHandler = handlerMap.remove(sessionId);
		if (smscSmppSessionHandler != null) {
			smscSmppSessionHandler.destroy();
		}
		// make sure it's really shutdown
		session.destroy();
	}

}
