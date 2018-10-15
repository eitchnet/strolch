package li.strolch.handler.operationslog;

import java.util.*;
import java.util.concurrent.ExecutorService;

import li.strolch.agent.api.ComponentContainer;
import li.strolch.agent.api.StrolchComponent;
import li.strolch.model.Locator;
import li.strolch.persistence.api.LogMessageDao;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.runtime.configuration.ComponentConfiguration;

public class OperationsLog extends StrolchComponent {

	private Map<String, List<LogMessage>> logMessagesByRealmAndId;
	private Map<String, LinkedHashMap<Locator, LinkedHashSet<LogMessage>>> logMessagesByLocator;
	private int maxMessages;
	private ExecutorService executorService;

	public OperationsLog(ComponentContainer container, String componentName) {
		super(container, componentName);
	}

	@Override
	public void initialize(ComponentConfiguration configuration) throws Exception {

		this.maxMessages = configuration.getInt("maxMessages", 10000);

		this.logMessagesByRealmAndId = new HashMap<>();
		this.logMessagesByLocator = new HashMap<>();

		this.executorService = getExecutorService("OperationsLog");

		super.initialize(configuration);
	}

	@Override
	public void start() throws Exception {
		runAsAgent(ctx -> {
			Set<String> realmNames = getContainer().getRealmNames();
			for (String realmName : realmNames) {

				// ignore for transient realms
				if (getContainer().getRealm(realmName).getMode().isTransient())
					continue;

				logger.info("Loading OperationsLog for realm " + realmName + "...");

				try (StrolchTransaction tx = openTx(realmName, ctx.getCertificate())) {
					LogMessageDao logMessageDao = tx.getPersistenceHandler().getLogMessageDao(tx);
					List<LogMessage> messages = logMessageDao.queryLatest(realmName, this.maxMessages);
					logger.info("Loaded " + messages.size() + " messages for OperationsLog for realm " + realmName);
					this.logMessagesByRealmAndId.put(realmName, messages);
				}
			}
		});

		super.start();
	}

	public void setMaxMessages(int maxMessages) {
		this.maxMessages = maxMessages;
	}

	public synchronized void addMessage(LogMessage logMessage) {

		// store in global list
		List<LogMessage> logMessages = this.logMessagesByRealmAndId
				.computeIfAbsent(logMessage.getRealm(), r -> new ArrayList<>());
		logMessages.add(logMessage);

		// store under locator
		LinkedHashMap<Locator, LinkedHashSet<LogMessage>> logMessagesLocator = this.logMessagesByLocator
				.computeIfAbsent(logMessage.getRealm(), this::newBoundedLocatorMap);
		LinkedHashSet<LogMessage> messages = logMessagesLocator
				.computeIfAbsent(logMessage.getLocator(), (l) -> new LinkedHashSet<>());
		messages.add(logMessage);

		this.executorService.submit(() -> this.persistAndPrune(logMessages, logMessage));
	}

	private void persistAndPrune(List<LogMessage> logMessages, LogMessage logMessage) {
		runAsAgent(ctx -> {

			List<LogMessage> messagesToRemove = null;
			synchronized (this) {
				if (logMessages.size() > this.maxMessages) {
					messagesToRemove = new ArrayList<>();
					int maxDelete = Math.max(1, (int) (this.maxMessages * 0.1));
					logger.info("Pruning " + maxDelete + " messages from OperationsLog...");
					Iterator<LogMessage> iterator = logMessages.iterator();
					while (maxDelete > 0 && iterator.hasNext()) {
						LogMessage messageToRemove = iterator.next();
						messagesToRemove.add(messageToRemove);
						iterator.remove();
						maxDelete--;
					}
				}

				// only for persisted realms
				if (!getContainer().getRealm(logMessage.getRealm()).getMode().isTransient()) {
					try (StrolchTransaction tx = openTx(logMessage.getRealm(), ctx.getCertificate())) {
						LogMessageDao logMessageDao = tx.getPersistenceHandler().getLogMessageDao(tx);
						if (messagesToRemove != null && !messagesToRemove.isEmpty())
							logMessageDao.removeAll(messagesToRemove);
						logMessageDao.save(logMessage);
						tx.commitOnClose();
					}
				}
			}
		});
	}

	public synchronized void clearMessages(String realm, Locator locator) {
		LinkedHashMap<Locator, LinkedHashSet<LogMessage>> logMessages = this.logMessagesByLocator.get(realm);
		if (logMessages != null)
			logMessages.remove(locator);
	}

	public synchronized Optional<Set<LogMessage>> getMessagesFor(String realm, Locator locator) {
		LinkedHashMap<Locator, LinkedHashSet<LogMessage>> logMessages = this.logMessagesByLocator.get(realm);
		if (logMessages == null)
			return Optional.empty();
		return Optional.ofNullable(logMessages.get(locator));
	}

	public synchronized List<LogMessage> getMessages(String realm) {
		List<LogMessage> logMessages = this.logMessagesByRealmAndId.get(realm);
		if (logMessages == null)
			return Collections.emptyList();

		return new ArrayList<>(logMessages);
	}

	private LinkedHashMap<Locator, LinkedHashSet<LogMessage>> newBoundedLocatorMap(String realm) {
		return new LinkedHashMap<Locator, LinkedHashSet<LogMessage>>() {
			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<Locator, LinkedHashSet<LogMessage>> eldest) {
				return size() > maxMessages;
			}
		};
	}
}
