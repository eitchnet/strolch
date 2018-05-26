/*
 * Copyright 2013 Robert von Burg <eitch@eitchnet.ch>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package li.strolch.persistence.api;

import java.util.List;
import java.util.stream.Stream;

import li.strolch.agent.api.*;
import li.strolch.agent.impl.DataStoreMode;
import li.strolch.exception.StrolchException;
import li.strolch.exception.StrolchModelException;
import li.strolch.model.*;
import li.strolch.model.activity.Action;
import li.strolch.model.activity.Activity;
import li.strolch.model.audit.AccessType;
import li.strolch.model.audit.Audit;
import li.strolch.model.audit.AuditVisitor;
import li.strolch.model.parameter.Parameter;
import li.strolch.model.parameter.StringListParameter;
import li.strolch.model.parameter.StringParameter;
import li.strolch.model.query.ActivityQuery;
import li.strolch.model.query.AuditQuery;
import li.strolch.model.query.OrderQuery;
import li.strolch.model.query.ResourceQuery;
import li.strolch.model.visitor.ActivityVisitor;
import li.strolch.model.visitor.OrderVisitor;
import li.strolch.model.visitor.ResourceVisitor;
import li.strolch.privilege.base.AccessDeniedException;
import li.strolch.privilege.model.Certificate;
import li.strolch.privilege.model.PrivilegeContext;
import li.strolch.runtime.StrolchConstants;
import li.strolch.service.api.Command;

/**
 * <p>
 * {@link StrolchTransaction} is the central element in Strolch. It gives the developer access to the Strolch model and
 * performs all the required actions to keep the model consistent etc.
 * </p>
 *
 * <p>
 * A Strolch transaction is performed as follows as it is an {@link AutoCloseable} implementation
 * </p>
 *
 * <pre>
 * StrolchAgent strolchAgent = getStrolchAgent();
 * StrolchRealm realm = strolchAgent.getContainer().getRealm(StrolchConstants.DEFAULT_REALM);
 * try (StrolchTransaction tx = realm.openTx(certificate, getClass())) {
 *   // do work e.g. add commands
 *   tx.commitOnClose();
 * }
 * </pre>
 *
 * <p>
 * A {@link StrolchTransaction} is always opened for a specific realm, should no specific realms be configured, then the
 * {@link StrolchConstants#DEFAULT_REALM} is automatically created.
 * </p>
 *
 * <p>
 * A {@link StrolchTransaction} takes care of the following:
 * </p>
 *
 * <ul>
 * <li>Opening and closing database connections</li>
 * <li>Releasing locks to strolch elements, if {@link #lock(StrolchRootElement)} or {@link #lock(Locator)} is used</li>
 * <li>Performing Commands correctly</li>
 * <li>exception handling</li>
 * <li>auditing</li>
 * <li>updating observers</li>
 * </ul>
 *
 * @author Robert von Burg <eitch@eitchnet.ch>
 * @see AbstractTransaction
 */
public interface StrolchTransaction extends AutoCloseable {

	/**
	 * Returns the action for which this transaction was opened
	 *
	 * @return the action for which this transaction was opened
	 */
	public String getAction();

	/**
	 * Returns the name of the {@link StrolchRealm} for which this transaction was opened
	 *
	 * @return the name of the {@link StrolchRealm} for which this transaction was opened
	 */
	public String getRealmName();

	/**
	 * Returns the {@link Certificate} which allowed this TX to be opened
	 *
	 * @return the {@link Certificate} which allowed this TX to be opened
	 */
	public Certificate getCertificate();

	/**
	 * Returns the {@link PrivilegeContext} for this TX's certificate
	 *
	 * @return the {@link PrivilegeContext} for this TX's certificate
	 */
	public PrivilegeContext getPrivilegeContext();

	/**
	 * Returns a reference to the {@link AuditTrail} for the {@link StrolchRealm} for which this transaction was opened
	 *
	 * @return the {@link AuditTrail}
	 */
	public AuditTrail getAuditTrail();

	/**
	 * Returns a reference to the {@link ResourceMap} for the {@link StrolchRealm} for which this transaction was
	 * opened
	 *
	 * @return the {@link ResourceMap}
	 */
	public ResourceMap getResourceMap();

	/**
	 * Returns a reference to the {@link OrderMap} for the {@link StrolchRealm} for which this transaction was opened
	 *
	 * @return the {@link OrderMap}
	 */
	public OrderMap getOrderMap();

	/**
	 * Returns a reference to the {@link ActivityMap} for the {@link StrolchRealm} for which this transaction was
	 * opened
	 *
	 * @return the {@link ActivityMap}
	 */
	public ActivityMap getActivityMap();

	/**
	 * Returns the {@link PersistenceHandler}. If the {@link StrolchRealm} is not running in {@link
	 * DataStoreMode#TRANSIENT} mode, then the {@link PersistenceHandler} will be a {@link StrolchComponent}, otherwise
	 * it will be the internal in memory persistence handler
	 *
	 * @return the {@link PersistenceHandler}
	 */
	public PersistenceHandler getPersistenceHandler();

	/**
	 * Return the {@link ComponentContainer}
	 *
	 * @return the reference to the container
	 */
	public ComponentContainer getContainer();

	/**
	 * Returns the currently set {@link TransactionCloseStrategy}
	 *
	 * @return the currently set {@link TransactionCloseStrategy}
	 */
	public TransactionCloseStrategy getCloseStrategy();

	/**
	 * DO NOT CALL THIS METHOD. If the currently set close strategy is {@link TransactionCloseStrategy#READ_ONLY}, then
	 * when the transaction is closed, this method is called no changes to the model is performed but locks on objects
	 * are released and any other resources are released
	 */
	public void autoCloseableReadOnly() throws StrolchTransactionException;

	/**
	 * DO NOT CALL THIS METHOD. If the currently set close strategy is {@link TransactionCloseStrategy#COMMIT}, then
	 * when the transaction is closed, this method is called and all registered {@link Command} are performed, locks on
	 * objects are released and any other resources are released
	 */
	public void autoCloseableCommit() throws StrolchTransactionException;

	/**
	 * DO NOT CALL THIS METHOD. If the currently set close strategy is {@link TransactionCloseStrategy#ROLLBACK}, then
	 * when the transaction is closed, no further actions are performed and any {@link Command} which were performed
	 * have their {@link Command#undo()} method called and any DB connections are also rolled back
	 */
	public void autoCloseableRollback() throws StrolchTransactionException;

	/**
	 * <p>
	 * DO NOT CALL THIS METHOD. This interface implements {@link AutoCloseable} and transactions are expected to be used
	 * in a auto closing try block:
	 * </p>
	 * <p>
	 * <code>
	 * StrolchAgent strolchAgent = getStrolchAgent(); StrolchRealm realm = strolchAgent.getContainer().getRealm("defaultRealm");
	 * try(StrolchTransaction tx = realm.openTx(certificate, getClass())){ // do work tx.commitOnClose(); }
	 * </code>
	 * <p>
	 * After the block is closed, the transaction is automatically closed and all allocated resources are released
	 */
	@Override
	public void close() throws StrolchTransactionException;

	/**
	 * Sets the {@link TransactionCloseStrategy} to {@link TransactionCloseStrategy#READ_ONLY}
	 */
	public void doNothingOnClose();

	/**
	 * Sets the {@link TransactionCloseStrategy} to {@link TransactionCloseStrategy#COMMIT}
	 */
	public void commitOnClose();

	/**
	 * Sets the {@link TransactionCloseStrategy} to {@link TransactionCloseStrategy#ROLLBACK}
	 */
	public void rollbackOnClose();

	/**
	 * Sets the {@link TransactionCloseStrategy} to {@link TransactionCloseStrategy#ROLLBACK} and returns a {@link
	 * StrolchTransactionException} which can be thrown by the caller to stop the exception
	 *
	 * @param exceptionMessage
	 *
	 * @return a {@link StrolchTransactionException} to be thrown by the caller
	 */
	public StrolchTransactionException fail(String exceptionMessage);

	/**
	 * Clears the transaction element cache
	 */
	public void clearCache();

	/**
	 * <p>
	 * Performs all registered commands
	 * </p>
	 *
	 * <p>
	 * This method does not release any locks, nor does it notify any observers
	 * </p>
	 */
	public void flush();

	/**
	 * @return the current state of the transaction
	 *
	 * @see TransactionState
	 */
	public TransactionState getState();

	/**
	 * @return if the current state of the StrolchTransaction is {@link TransactionState#OPEN}
	 */
	public boolean isOpen();

	/**
	 * @return if the current state of the StrolchTransaction is {@link TransactionState#ROLLING_BACK}
	 */
	public boolean isRollingBack();

	/**
	 * @return if the current state of the StrolchTransaction is {@link TransactionState#COMMITTING}
	 */
	public boolean isCommitting();

	/**
	 * @return if the current state of the StrolchTransaction is {@link TransactionState#CLOSING}
	 */
	public boolean isClosing();

	/**
	 * @return if the current state of the StrolchTransaction is {@link TransactionState#CLOSED}
	 */
	public boolean isClosed();

	/**
	 * @return if the current state of the StrolchTransaction is {@link TransactionState#FAILED}
	 */
	public boolean isFailed();

	/**
	 * If the given argument is true, then no observer updates are performed
	 *
	 * @param suppressUpdates
	 * 		true to suppress the updates, false to enable them
	 */
	public void setSuppressUpdates(boolean suppressUpdates);

	/**
	 * Returns true if the observer updates are currently suppressed
	 *
	 * @return true if the observer updates are currently suppressed
	 */
	public boolean isSuppressUpdates();

	/**
	 * If the given argument is true, then no {@link Audit Audits} are written
	 *
	 * @param suppressAudits
	 * 		true to suppress writing {@link Audit Audits}, false to enable them
	 */
	public void setSuppressAudits(boolean suppressAudits);

	/**
	 * If the given argument is true, then no {@link Audit Audits} for Audits are written. Since the {@link AuditTrail}
	 * is also audited, {@link Audit Audits} for Audits are generated, this allows to suppress this should that be
	 * required.
	 *
	 * @param suppressAuditsForAudits
	 * 		true to suppress writing {@link Audit Audits}, false to enable them
	 */
	public void setSuppressAuditsForAudits(boolean suppressAuditsForAudits);

	/**
	 * Returns true if writing {@link Audit Audits} is currently suppressed
	 *
	 * @return true if writing {@link Audit Audits} is currently suppressed
	 */
	public boolean isSuppressAudits();

	/**
	 * Returns true if writing {@link Audit Audits} for Audits is currently suppressed
	 *
	 * @return true if writing {@link Audit Audits} for Audits is currently suppressed
	 */
	public boolean isSuppressAuditsForAudits();

	/**
	 * If the given argument is true, then logging of a {@link TransactionCloseStrategy#READ_ONLY} will be suppressed
	 *
	 * @param suppressDoNothingLogging
	 * 		true to suppress logging of a {@link TransactionCloseStrategy#READ_ONLY}, false to enable logging
	 */
	void setSuppressDoNothingLogging(boolean suppressDoNothingLogging);

	/**
	 * Returns true if logging of a {@link TransactionCloseStrategy#READ_ONLY} should be suppressed
	 *
	 * @return true if logging of a {@link TransactionCloseStrategy#READ_ONLY} should be suppressed
	 */
	boolean isSuppressDoNothingLogging();

	/**
	 * Returns true if versioning is enabled on the {@link StrolchRealm} for which this transaction has been opened
	 *
	 * @return true if versioning is enabled
	 */
	boolean isVersioningEnabled();

	/**
	 * Locks the element with the given locator and registers it on the transaction so the lock is released when the
	 * transaction is closed
	 *
	 * @param locator
	 * 		the {@link Locator} of the element to lock
	 *
	 * @throws StrolchLockException
	 * 		if something goes wrong while locking
	 */
	public void lock(Locator locator) throws StrolchLockException;

	/**
	 * Locks the given element and registers it on the transaction so the lock is released when the transaction is
	 * closed
	 *
	 * @param element
	 * 		the element to lock
	 *
	 * @throws StrolchLockException
	 * 		if something goes wrong while locking
	 */
	public <T extends StrolchRootElement> void lock(T element) throws StrolchLockException;

	/**
	 * Releases the lock of the element so that even though the transaction is still open, another thread/transaction
	 * can lock the element
	 *
	 * @param element
	 * 		the element for which the lock is to be released
	 *
	 * @throws StrolchLockException
	 * 		if something goes wrong while unlocking
	 */
	public <T extends StrolchRootElement> void releaseLock(T element) throws StrolchLockException;

	/**
	 * Releases the lock of the element with the given {@link Locator} so that even though the transaction is still
	 * open, another thread/transaction can lock the element
	 *
	 * @param locator
	 * 		the {@link Locator} of the element for which the lock is to be released
	 *
	 * @throws StrolchLockException
	 * 		if something goes wrong while unlocking
	 */
	public <T extends StrolchRootElement> void releaseLock(Locator locator) throws StrolchLockException;

	/**
	 * Adds the given {@link Command} to the transaction. Using this method guarantees that a {@link Command} is
	 * executed properly:
	 * <ul>
	 * <li>{@link Command#validate()}</li>
	 * <li>{@link Command#doCommand()}</li>
	 * </ul>
	 *
	 * and if an exception occurs:
	 * <ul>
	 * <li>{@link Command#undo()}</li>
	 * </ul>
	 *
	 * @param command
	 * 		the command to add
	 */
	public void addCommand(Command command);

	/**
	 * Helper method to create an {@link Audit} with the given arguments. The audit can then be saved by calling {@link
	 * AuditTrail#add(StrolchTransaction, Audit)}
	 *
	 * @param accessType
	 * 		the type of access
	 * @param elementType
	 * 		the element type, i.e. {@link Tags#RESOURCE}, {@link Tags#ORDER}
	 * @param elementSubType
	 * 		the element sub type, e.g. {@link Resource#getType()}
	 * @param id
	 * 		the id of the element audited
	 *
	 * @return the new audit
	 */
	public Audit auditFrom(AccessType accessType, String elementType, String elementSubType, String id);

	/**
	 * Helper method to create an {@link Audit} with the given arguments. The audit can then be saved by calling {@link
	 * AuditTrail#add(StrolchTransaction, Audit)}
	 *
	 * @param accessType
	 * 		the type of access
	 * @param element
	 * 		the element from which to to create the audit
	 *
	 * @return the new audit
	 */
	public Audit auditFrom(AccessType accessType, StrolchRootElement element);

	/**
	 * <p>
	 * Performs the given {@link OrderQuery} and each returned {@link Order} is passed through the {@link OrderVisitor}
	 * attached to the {@link OrderQuery} and the return value of the visitor is added to the return list
	 * </p>
	 *
	 * @param query
	 * 		the query to perform
	 *
	 * @return the result list of elements as returned by the {@link OrderVisitor}, never null
	 */
	public <U> List<U> doQuery(OrderQuery<U> query);

	/**
	 * <p>
	 * Performs the given {@link ResourceQuery} and each returned {@link Resource} is passed through the {@link
	 * ResourceVisitor} attached to the {@link ResourceQuery} and the return value of the visitor is added to the return
	 * list
	 * </p>
	 *
	 * @param query
	 * 		the query to perform
	 *
	 * @return the result list of elements as returned by the {@link ResourceVisitor}, never null
	 */
	public <U> List<U> doQuery(ResourceQuery<U> query);

	/**
	 * <p>
	 * Performs the given {@link ActivityQuery} and each returned {@link Activity} is passed through the {@link
	 * ActivityVisitor} attached to the {@link ActivityQuery} and the return value of the visitor is added to the return
	 * list
	 * </p>
	 *
	 * @param query
	 * 		the query to perform
	 *
	 * @return the result list of elements as returned by the {@link ActivityVisitor}, never null
	 */
	public <U> List<U> doQuery(ActivityQuery<U> query);

	/**
	 * <p>
	 * Performs the given {@link AuditQuery} and each returned {@link Audit} is passed through the {@link AuditVisitor}
	 * attached to the {@link AuditQuery} and the return value of the visitor is added to the return list
	 * </p>
	 *
	 * @param query
	 * 		the query to perform
	 *
	 * @return the result list of elements as returned by the {@link AuditVisitor}, never null
	 */
	public <U> List<U> doQuery(AuditQuery<U> query);

	/**
	 * <p>
	 * Used to find a {@link StrolchElement} by a {@link Locator}, throwing exception if the element is not found
	 * </p>
	 *
	 * @throws StrolchException
	 * 		if the element could not be found
	 * @see #findElement(Locator, boolean)
	 */
	public <T extends StrolchElement> T findElement(Locator locator) throws StrolchException, ClassCastException;

	/**
	 * <p>
	 * Used to find a {@link StrolchElement} by a {@link Locator}.
	 * </p>
	 *
	 * <p>
	 * A Locator has the form <i>&lt;ObjectClassType&gt;/&lt;Type&gt;/&lt;Id&gt;</i> - this is the least amount of path
	 * elements to find an object. Thus to query a {@link Resource} of type "MyType" and the id "@1" use the following
	 * path: <i>Resourcee/MyType/@1</i>
	 * </p>
	 *
	 * <p>
	 * This method can also be used to find a deeper element, e.g. a specific {@link Parameter} on an {@link
	 * ParameterBag} on an {@link Order}. This would be done as follows: <i>Order/MyType/Bag/@1/myParam</i>
	 * </p>
	 *
	 * @param locator
	 * 		the locator defining the path to the element which is to be found
	 * @param allowNull
	 * 		if element not found, null is returned - presuming Locator has a valid form
	 *
	 * @return the element described by the locator. If {@link Locator} references an element which does not exist, i.e.
	 * an inexistant {@link Resource} or an inexistand {@link Parameter} on a Resource, then a {@link StrolchException}
	 * is thrown
	 *
	 * @throws StrolchException
	 * 		if the element could not be found and {@code allowNull} is false
	 * @throws ClassCastException
	 * 		if the querying code is not asking for the correct instance. Do not query a {@link Parameter} if the variable
	 * 		to which the result is to be is stored is a {@link Resource}, etc.
	 */
	public <T extends StrolchElement> T findElement(Locator locator, boolean allowNull)
			throws StrolchException, ClassCastException;

	/**
	 * Returns a stream of resources for the given types, if empty, streams all possible types
	 *
	 * @param types
	 * 		the types of resources to return in the stream, if empty, streams all possible types
	 *
	 * @return a stream of resources
	 */
	public Stream<Resource> streamResources(String... types);

	/**
	 * Returns a stream of orders for the given types, if empty, streams all possible types
	 *
	 * @param types
	 * 		the types of orders to return in the stream, if empty, streams all possible types
	 *
	 * @return a stream of orders
	 */
	public Stream<Order> streamOrders(String... types);

	/**
	 * Returns a stream of activities for the given types, if empty, streams all possible types
	 *
	 * @param types
	 * 		the types of activities to return in the stream, if empty, streams all possible types
	 *
	 * @return a stream of activities
	 */
	public Stream<Activity> streamActivities(String... types);

	/**
	 * <p>
	 * Returns a copy of the {@link Resource} of Type {@link StrolchConstants#TEMPLATE} with the given type as id, or
	 * null if it does not exist
	 * </p>
	 *
	 * <p>
	 * Templates are {@link StrolchRootElement StrolchRootElements} which have the type {@link
	 * StrolchConstants#TEMPLATE} and their id is the type of element for which it is a template. For instance when
	 * creating a {@link Resource} of type {@code Person} then having a template with the id {@code Person} helps
	 * creating new Person resources; get the resource and then create a clone: {@link Resource#getClone()}
	 * </p>
	 *
	 * @param type
	 * 		the id of the {@link Resource} template
	 *
	 * @return the copy of the {@link Resource} template with the given id, or null if it does not exist
	 */
	public Resource getResourceTemplate(String type);

	/**
	 * <p>
	 * Returns a copy of the {@link Resource} of Type {@link StrolchConstants#TEMPLATE} with the given type as id. If
	 * {@code assertExists} is true, then an exception is thrown if the template does not exist does not exist
	 * </p>
	 *
	 * <p>
	 * Templates are {@link StrolchRootElement StrolchRootElements} which have the type {@link
	 * StrolchConstants#TEMPLATE} and their id is the type of element for which it is a template. For instance when
	 * creating a {@link Resource} of type {@code Person} then having a template with the id {@code Person} helps
	 * creating new Person resources; get the resource and then create a clone: {@link Resource#getClone()}
	 * </p>
	 *
	 * @param type
	 * 		the id of the {@link Resource} template
	 *
	 * @return the copy of the {@link Resource} template with the given id, or if {@code assertExists} is true, then an
	 * exception is thrown if the resource does not exist
	 *
	 * @throws StrolchException
	 * 		if the template does not exist and {@code assertExists} is true
	 */
	public Resource getResourceTemplate(String type, boolean assertExists) throws StrolchException;

	/**
	 * <p>
	 * Returns a copy of the {@link Order} of Type {@link StrolchConstants#TEMPLATE} with the given type as id, or null
	 * if it does not exist
	 * </p>
	 *
	 * <p>
	 * Templates are {@link StrolchRootElement StrolchRootElements} which have the type {@link
	 * StrolchConstants#TEMPLATE} and their id is the type of element for which it is a template. For instance when
	 * creating an {@link Order} of type {@code PurchaseOrder} then having a template with the id {@code PurchaseOrder}
	 * helps creating new PurchaseOrder orders; get the order and then create a clone: {@link Order#getClone()}
	 * </p>
	 *
	 * @param type
	 * 		the id of the {@link Order} template
	 *
	 * @return the copy of the {@link Order} template with the given id, or null if it does not exist
	 */
	public Order getOrderTemplate(String type);

	/**
	 * <p>
	 * Returns a copy of the {@link Order} of Type {@link StrolchConstants#TEMPLATE} with the given type as id. If
	 * {@code assertExists} is true, then an exception is thrown if the template does not exist does not exist
	 * </p>
	 *
	 * <p>
	 * Templates are {@link StrolchRootElement StrolchRootElements} which have the type {@link
	 * StrolchConstants#TEMPLATE} and their id is the type of element for which it is a template. For instance when
	 * creating an {@link Order} of type {@code PurchaseOrder} then having a template with the id {@code PurchaseOrder}
	 * helps creating new PurchaseOrder orders; get the order and then create a clone: {@link Order#getClone()}
	 * </p>
	 *
	 * @param type
	 * 		the id of the {@link Order} template
	 *
	 * @return the copy of the {@link Order} template with the given id, or if {@code assertExists} is true, then an
	 * exception is thrown if the order does not exist
	 *
	 * @throws StrolchException
	 * 		if the template does not exist and {@code assertExists} is true
	 */
	public Order getOrderTemplate(String type, boolean assertExists) throws StrolchException;

	/**
	 * <p>
	 * Returns a copy of the {@link Activity} of Type {@link StrolchConstants#TEMPLATE} with the given type as id, or
	 * null if it does not exist
	 * </p>
	 *
	 * <p>
	 * Templates are {@link StrolchRootElement StrolchRootElements} which have the type {@link
	 * StrolchConstants#TEMPLATE} and their id is the type of element for which it is a template. For instance when
	 * creating a {@link Activity} of type {@code ToStock} then having a template with the id {@code ToStock} helps
	 * creating new ToStock activities; get the activity and then create a clone: {@link Activity#getClone()}
	 * </p>
	 *
	 * @param type
	 * 		the id of the {@link Activity} template
	 *
	 * @return the copy of the {@link Activity} template with the given id, or null if it does not exist
	 */
	public Activity getActivityTemplate(String type);

	/**
	 * <p>
	 * Returns a copy of the {@link Activity} of Type {@link StrolchConstants#TEMPLATE} with the given type as id. If
	 * {@code assertExists} is true, then an exception is thrown if the template does not exist does not exist
	 * </p>
	 *
	 * <p>
	 * Templates are {@link StrolchRootElement StrolchRootElements} which have the type {@link
	 * StrolchConstants#TEMPLATE} and their id is the type of element for which it is a template. For instance when
	 * creating a {@link Activity} of type {@code ToStock} then having a template with the id {@code ToStock} helps
	 * creating new ToStock activities; get the activity and then create a clone: {@link Activity#getClone()}
	 * </p>
	 *
	 * @param type
	 * 		the id of the {@link Activity} template
	 *
	 * @return the copy of the {@link Activity} template with the given id, or if {@code assertExists} is true, then an
	 * exception is thrown if the activity does not exist
	 *
	 * @throws StrolchException
	 * 		if the template does not exist and {@code assertExists} is true
	 */
	public Activity getActivityTemplate(String type, boolean assertExists) throws StrolchException;

	/**
	 * Returns the {@link Resource} with the given type and id, or null if it does not exist
	 *
	 * @param type
	 * 		the type of the {@link Resource}
	 * @param id
	 * 		the id of the {@link Resource}
	 *
	 * @return the {@link Resource} with the given type and id, or null if it does not exist
	 */
	public Resource getResourceBy(String type, String id);

	/**
	 * Returns the {@link Resource} with the given type and id, or null if it does not exist
	 *
	 * @param type
	 * 		the type of the {@link Resource}
	 * @param id
	 * 		the id of the {@link Resource}
	 * @param assertExists
	 * 		if true, and resource does not exist, then a {@link StrolchException} is thrown
	 *
	 * @return the {@link Resource} with the given type and id, or null if it does not exist
	 *
	 * @throws StrolchException
	 * 		if the resource does not exist, and assertExists is true
	 */
	public Resource getResourceBy(String type, String id, boolean assertExists) throws StrolchException;

	/**
	 * Returns the {@link Resource} which is referenced by the given {@link StringParameter}. A reference {@link
	 * Parameter} must have its interpretation set to {@link StrolchConstants#INTERPRETATION_RESOURCE_REF} and the UOM
	 * must be set to the resource's type and the value is the id of the resource
	 *
	 * @param refP
	 * 		the {@link StringParameter} which references a {@link Resource}
	 *
	 * @return the resource referenced by the parameter, or null if it does not exist
	 *
	 * @throws StrolchException
	 * 		if the {@link StringParameter} is not a properly configured as a reference parameter
	 */
	public Resource getResourceBy(StringParameter refP) throws StrolchException;

	/**
	 * Returns the {@link Resource} which is referenced by the given {@link StringParameter}. A reference {@link
	 * Parameter} must have its interpretation set to {@link StrolchConstants#INTERPRETATION_RESOURCE_REF} and the UOM
	 * must be set to the resource's type and the value is the id of the resource
	 *
	 * @param refP
	 * 		the {@link StringParameter} which references a {@link Resource}
	 * @param assertExists
	 * 		if true, and resource does not exist, then a {@link StrolchException} is thrown
	 *
	 * @return the resource referenced by the parameter, or null if it does not exist
	 *
	 * @throws StrolchException
	 * 		if the {@link StringParameter} is not a properly configured as a reference parameter, or if the resource does
	 * 		not exist, and assertExists is true
	 */
	public Resource getResourceBy(StringParameter refP, boolean assertExists) throws StrolchException;

	/**
	 * Returns all {@link Resource Resources} which are referenced by the given {@link StringListParameter}. A reference
	 * {@link Parameter} must have its interpretation set to {@link StrolchConstants#INTERPRETATION_RESOURCE_REF} and
	 * the UOM must be set to the resource's type and the value is the id of the resource
	 *
	 * @param refP
	 * 		the {@link StringListParameter} which references a list of {@link Resource Resources}
	 *
	 * @return the resources referenced by the parameter, or the empty list if they do not exist. <b>Note:</b> Any
	 * missing resources are not returned!
	 *
	 * @throws StrolchException
	 * 		if the {@link StringListParameter} is not a properly configured as a reference parameter
	 */
	public List<Resource> getResourcesBy(StringListParameter refP) throws StrolchException;

	/**
	 * Returns all {@link Resource Resources} which are referenced by the given {@link StringListParameter}. A reference
	 * {@link Parameter} must have its interpretation set to {@link StrolchConstants#INTERPRETATION_RESOURCE_REF} and
	 * the UOM must be set to the resource's type and the value is the id of the resource
	 *
	 * @param refP
	 * 		the {@link StringListParameter} which references a list of {@link Resource Resources}
	 * @param assertExists
	 * 		if true, and resource does not exist, then a {@link StrolchException} is thrown
	 *
	 * @return the resources referenced by the parameter, or the empty list if they do not exist. <b>Note:</b> Any
	 * missing resources are not returned unless {@code assertExists} is true
	 *
	 * @throws StrolchException
	 * 		if the {@link StringListParameter} is not a properly configured as a reference parameter
	 */
	public List<Resource> getResourcesBy(StringListParameter refP, boolean assertExists) throws StrolchException;

	/**
	 * <p>Returns the {@link Resource} for the given {@link Action}. This is done by getting the resource by {@link
	 * Action#getResourceType()} and {@link Action#getResourceId()}</p>
	 *
	 * <p>Should the resource not exist, then null is returned</p>
	 *
	 * @param action
	 * 		the action for which to return the resoruce
	 *
	 * @return the resource referenced by the action, or null if it does not exist
	 *
	 * @throws StrolchException
	 * 		if the action is null, or something else goes wrong
	 */
	public Resource getResourceFor(Action action) throws StrolchException;

	/**
	 * <p>Returns the {@link Resource} for the given {@link Action}. This is done by getting the resource by {@link
	 * Action#getResourceType()} and {@link Action#getResourceId()}</p>
	 *
	 * <p>Should the resource not exist and {@code assertExists} is true, then an exception is thrown, otherwise
	 * null is returned</p>
	 *
	 * @param action
	 * 		the action for which to return the resoruce
	 *
	 * @return the resource referenced by the action. If the resource does not exist and {@code assertExists} is true
	 * then an exception is thrown, otherwise null is returnee
	 *
	 * @throws StrolchException
	 * 		if the action is null and {@code assertExists} is true, or something else goes wrong
	 */
	public Resource getResourceFor(Action action, boolean assertExists) throws StrolchException;

	/**
	 * Returns the {@link Activity} with the given type and id, or null if it does not exist
	 *
	 * @param type
	 * 		the type of the {@link Activity}
	 * @param id
	 * 		the id of the {@link Activity}
	 *
	 * @return the {@link Activity} with the given type and id, or null if it does not exist
	 */
	public Activity getActivityBy(String type, String id);

	/**
	 * Returns the {@link Activity} with the given type and id, or null if it does not exist
	 *
	 * @param type
	 * 		the type of the {@link Activity}
	 * @param id
	 * 		the id of the {@link Activity}
	 * @param assertExists
	 * 		if true, and activity does not exist, then a {@link StrolchException} is thrown
	 *
	 * @return the {@link Activity} with the given type and id, or null if it does not exist
	 *
	 * @throws StrolchException
	 * 		if the activity does not exist, and assertExists is true
	 */
	public Activity getActivityBy(String type, String id, boolean assertExists) throws StrolchException;

	/**
	 * Returns the {@link Activity} which is referenced by the given {@link StringParameter}. A reference {@link
	 * Parameter} must have its interpretation set to {@link StrolchConstants#INTERPRETATION_ACTIVITY_REF} and the UOM
	 * must be set to the activity's type and the value is the id of the activity
	 *
	 * @param refP
	 * 		the {@link StringParameter} which references an {@link Activity}
	 *
	 * @return the activity referenced by the parameter, or null if it does not exist
	 *
	 * @throws StrolchException
	 * 		if the {@link StringParameter} is not a properly configured as a reference parameter
	 */
	public Activity getActivityBy(StringParameter refP) throws StrolchException;

	/**
	 * Returns the {@link Activity} which is referenced by the given {@link StringParameter}. A reference {@link
	 * Parameter} must have its interpretation set to {@link StrolchConstants#INTERPRETATION_ACTIVITY_REF} and the UOM
	 * must be set to the activity's type and the value is the id of the activity
	 *
	 * @param refP
	 * 		the {@link StringParameter} which references an {@link Activity}
	 * @param assertExists
	 * 		if true, and activity does not exist, then a {@link StrolchException} is thrown
	 *
	 * @return the activity referenced by the parameter, or null if it does not exist
	 *
	 * @throws StrolchException
	 * 		if the {@link StringParameter} is not a properly configured as a reference parameter, or if the activity does
	 * 		not exist, and assertExists is true
	 */
	public Activity getActivityBy(StringParameter refP, boolean assertExists) throws StrolchException;

	/**
	 * Returns all {@link Activity Activities} which are referenced by the given {@link StringListParameter}. A
	 * reference {@link Parameter} must have its interpretation set to {@link StrolchConstants#INTERPRETATION_ACTIVITY_REF}
	 * and the UOM must be set to the activity's type and the value is the id of the activity
	 *
	 * @param refP
	 * 		the {@link StringListParameter} which references a list of {@link Activity Activities}
	 *
	 * @return the activities referenced by the parameter, or the empty list if they do not exist. <b>Note:</b> Any
	 * missing activities are not returned!
	 *
	 * @throws StrolchException
	 * 		if the {@link StringListParameter} is not a properly configured as a reference parameter
	 */
	public List<Activity> getActivitiesBy(StringListParameter refP) throws StrolchException;

	/**
	 * Returns all {@link Activity Activities} which are referenced by the given {@link StringListParameter}. A
	 * reference {@link Parameter} must have its interpretation set to {@link StrolchConstants#INTERPRETATION_ACTIVITY_REF}
	 * and the UOM must be set to the activity's type and the value is the id of the activity
	 *
	 * @param refP
	 * 		the {@link StringListParameter} which references a list of {@link Activity Activities}
	 * @param assertExists
	 * 		if true, and activity does not exist, then a {@link StrolchException} is thrown
	 *
	 * @return the activities referenced by the parameter, or the empty list if they do not exist. <b>Note:</b> Any
	 * missing activities are not returned unless {@code assertExists} is true
	 *
	 * @throws StrolchException
	 * 		if the {@link StringListParameter} is not a properly configured as a reference parameter
	 */
	public List<Activity> getActivitiesBy(StringListParameter refP, boolean assertExists) throws StrolchException;

	/**
	 * Returns the {@link Order} with the given type and id, or null if it does not exist
	 *
	 * @param type
	 * 		the type of the {@link Order}
	 * @param id
	 * 		the id of the {@link Order}
	 *
	 * @return the {@link Order} with the given type and id, or null if it does not exist
	 */
	public Order getOrderBy(String type, String id);

	/**
	 * Returns the {@link Order} with the given type and id, or null if it does not exist
	 *
	 * @param type
	 * 		the type of the {@link Order}
	 * @param id
	 * 		the id of the {@link Order}
	 * @param assertExists
	 * 		if true, and order does not exist, then a {@link StrolchException} is thrown
	 *
	 * @return the {@link Order} with the given type and id, or null if it does not exist
	 *
	 * @throws StrolchException
	 * 		if the order does not exist, and assertExists is true
	 */
	public Order getOrderBy(String type, String id, boolean assertExists) throws StrolchException;

	/**
	 * Returns the {@link Order} which is referenced by the given {@link StringParameter}. A reference {@link Parameter}
	 * must have its interpretation set to {@link StrolchConstants#INTERPRETATION_ORDER_REF} and the UOM must be set to
	 * the order's type and the value is the id of the order
	 *
	 * @param refP
	 * 		the {@link StringParameter} which references an {@link Order}
	 *
	 * @return the order referenced by the parameter, or null if it does not exist
	 *
	 * @throws StrolchException
	 * 		if the {@link StringParameter} is not a properly configured as a reference parameter
	 */
	public Order getOrderBy(StringParameter refP) throws StrolchException;

	/**
	 * Returns the {@link Order} which is referenced by the given {@link StringParameter}. A reference {@link Parameter}
	 * must have its interpretation set to {@link StrolchConstants#INTERPRETATION_ORDER_REF} and the UOM must be set to
	 * the order's type and the value is the id of the order
	 *
	 * @param refP
	 * 		the {@link StringParameter} which references an {@link Order}
	 * @param assertExists
	 * 		if true, and order does not exist, then a {@link StrolchException} is thrown
	 *
	 * @return the order referenced by the parameter, or null if it does not exist
	 *
	 * @throws StrolchException
	 * 		if the {@link StringParameter} is not a properly configured as a reference parameter, or if the order does not
	 * 		exist, and assertExists is true
	 */
	public Order getOrderBy(StringParameter refP, boolean assertExists) throws StrolchException;

	/**
	 * Returns all {@link Order Orders} which are referenced by the given {@link StringListParameter}. A reference
	 * {@link Parameter} must have its interpretation set to {@link StrolchConstants#INTERPRETATION_ORDER_REF} and the
	 * UOM must be set to the order's type and the value is the id of the order
	 *
	 * @param refP
	 * 		the {@link StringListParameter} which references a list of {@link Order Orders}
	 *
	 * @return the orders referenced by the parameter, or the empty list if they do not exist. <b>Note:</b> Any missing
	 * orders are not returned!
	 *
	 * @throws StrolchException
	 * 		if the {@link StringListParameter} is not a properly configured as a reference parameter
	 */
	public List<Order> getOrdersBy(StringListParameter refP) throws StrolchException;

	/**
	 * Returns all {@link Order Orders} which are referenced by the given {@link StringListParameter}. A reference
	 * {@link Parameter} must have its interpretation set to {@link StrolchConstants#INTERPRETATION_ORDER_REF} and the
	 * UOM must be set to the order's type and the value is the id of the order
	 *
	 * @param refP
	 * 		the {@link StringListParameter} which references a list of {@link Order Orders}
	 * @param assertExists
	 * 		if true, and order does not exist, then a {@link StrolchException} is thrown
	 *
	 * @return the orders referenced by the parameter, or the empty list if they do not exist. <b>Note:</b> Any missing
	 * orders are not returned unless {@code assertExists} is true
	 *
	 * @throws StrolchException
	 * 		if the {@link StringListParameter} is not a properly configured as a reference parameter
	 */
	public List<Order> getOrdersBy(StringListParameter refP, boolean assertExists) throws StrolchException;

	/**
	 * Returns true if the @{@link Resource} exists with the given type and ID
	 *
	 * @param type
	 * 		the type of Resource to check for
	 * @param id
	 * 		the ID of the Resource to check for
	 *
	 * @return true if the @{@link Resource} exists with the given type and ID
	 */
	public boolean hasResource(String type, String id);

	/**
	 * Returns true if the @{@link Order} exists with the given type and ID
	 *
	 * @param type
	 * 		the type of Order to check for
	 * @param id
	 * 		the ID of the Order to check for
	 *
	 * @return true if the @{@link Order} exists with the given type and ID
	 */
	public boolean hasOrder(String type, String id);

	/**
	 * Returns true if the @{@link Activity} exists with the given type and ID
	 *
	 * @param type
	 * 		the type of Activity to check for
	 * @param id
	 * 		the ID of the Activity to check for
	 *
	 * @return true if the @{@link Activity} exists with the given type and ID
	 */
	public boolean hasActivity(String type, String id);

	/**
	 * Adds and thus persists the given {@link Resource} by calling the relevant {@link Command}
	 *
	 * @param resource
	 * 		the resource to add
	 *
	 * @throws StrolchModelException
	 * 		if the resource is null, or a resource with the given ID already exists
	 */
	public void add(Resource resource) throws StrolchModelException;

	/**
	 * Adds and thus persists the given {@link Order} by calling the relevant {@link Command}
	 *
	 * @param order
	 * 		the order to add
	 *
	 * @throws StrolchModelException
	 * 		if the order is null, or an order with the given ID already exists
	 */
	public void add(Order order) throws StrolchException;

	/**
	 * Adds and thus persists the given {@link Activity} by calling the relevant {@link Command}
	 *
	 * @param activity
	 * 		the activity to add
	 *
	 * @throws StrolchModelException
	 * 		if the activity is null, or an activity with the given ID already exists
	 */
	public void add(Activity activity) throws StrolchException;

	/**
	 * Updates the given {@link Resource} by calling the relevant {@link Command}
	 *
	 * @param resource
	 * 		the resource to update
	 *
	 * @throws StrolchModelException
	 * 		if the resource is null
	 */
	public void update(Resource resource) throws StrolchException;

	/**
	 * Updates the given {@link Order} by calling the relevant {@link Command}
	 *
	 * @param order
	 * 		the order to update
	 *
	 * @throws StrolchModelException
	 * 		if the order is null
	 */
	public void update(Order order);

	/**
	 * Updates the given {@link Activity} by calling the relevant {@link Command}
	 *
	 * @param activity
	 * 		the activity to update
	 *
	 * @throws StrolchModelException
	 * 		if the activity is null
	 */
	public void update(Activity activity) throws StrolchException;

	/**
	 * Removes the given {@link Resource} by calling the relevant {@link Command}
	 *
	 * @param resource
	 * 		the resource to remove
	 *
	 * @throws StrolchModelException
	 * 		if the resource is null
	 */
	public void remove(Resource resource) throws StrolchException;

	/**
	 * Removes the given {@link Order} by calling the relevant {@link Command}
	 *
	 * @param order
	 * 		the order to remove
	 *
	 * @throws StrolchModelException
	 * 		if the order is null
	 */
	public void remove(Order order) throws StrolchException;

	/**
	 * Removes the given {@link Activity} by calling the relevant {@link Command}
	 *
	 * @param activity
	 * 		the activity to remove
	 *
	 * @throws StrolchModelException
	 * 		if the activity is null
	 */
	public void remove(Activity activity) throws StrolchException;

	/**
	 * Return true if this transaction has any registered elements or commands for persistence. E.g. {@link #add(Order)}
	 * or {@link #addCommand(Command)}, etc. were called
	 *
	 * @return true if this transaction needs to be committed to persist changes
	 */
	public boolean needsCommit();

	/**
	 * Asserts that the current {@link Certificate} has access to the given element with the given operation
	 *
	 * @param operation
	 * 		the operation to be performed
	 * @param element
	 * 		the element on which the operation is performed
	 *
	 * @throws AccessDeniedException
	 * 		if the session for this TX does not have the required privilege
	 */
	public void assertHasPrivilege(Operation operation, StrolchRootElement element) throws AccessDeniedException;
}
