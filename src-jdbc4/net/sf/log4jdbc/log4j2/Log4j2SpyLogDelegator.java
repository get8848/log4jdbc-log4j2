package net.sf.log4jdbc.log4j2;

import net.sf.log4jdbc.DriverSpy;
import net.sf.log4jdbc.ResultSetSpy;
import net.sf.log4jdbc.Spy;
import net.sf.log4jdbc.SpyLogDelegator;
import net.sf.log4jdbc.log4j2.message.ConnectionMessage;
import net.sf.log4jdbc.log4j2.message.ExceptionOccuredMessage;
import net.sf.log4jdbc.log4j2.message.MethodReturnedMessage;
import net.sf.log4jdbc.log4j2.message.SqlTimingOccurredMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;


/**
 * Delegates JDBC spy logging events to log4j2.
 * <p>
 * Differences in implementation and behavior as compared to <code>Slf4jSpyLogDelegator</code>: 
 * <ul>
 * <li>Only 2 loggers are used, instead of 6 in the <code>Slf4jSpyLogDelegator</code>: 
 * one for logging all spy logging events 
 * ("log4jdbc.log4j2", see <code>LOGGER</code> attribute), 
 * another one for logging debugging within log4jdbc itself 
 * ("log4jdbc.debug" logger, see <code>DEBUGLOGGER</code> attribute, 
 * or <code>Slf4jSpyLogDelegator</code> <code>debugLogger</code> attribute). 
 * <li>The behavior of the loggers "jdbc.connection", "jdbc.resultset", and "jdbc.audit" 
 * is reproduced through the use of <a href='http://logging.apache.org/log4j/2.0/manual/markers.html'>Markers</a> 
 * within one single logger: 
 * <ul>
 * <li>The <code>Marker</code> <code>CONNECTION_MARKER</code>, named "LOG4JDBC_CONNECTION"
 * <li>The <code>Marker</code> <code>RESULTSET_MARKER</code>, named "LOG4JDBC_RESULTSET", 
 * a child of the <code>Marker</code> <code>JDBC_MARKER</code>, named "LOG4JDBC_JDBC"
 * <li>The <code>Marker</code> <code>AUDIT_MARKER</code>, named "LOG4JDBC_AUDIT", 
 * a child of the <code>Marker</code> <code>JDBC_MARKER</code>, named "LOG4JDBC_JDBC"
 * </ul> 
 * <li>The behavior of the logger "jdbc.sqlonly" 
 * (see for instance <code>Slf4jSpyLogDelegator</code> <code>sqlOnlyLogger</code> attribute) 
 * is not reproduced. 
 * This is for the will of keeping one single logger, while the logging of spy events 
 * immediately, before the action is executed and before knowing the execution time, 
 * would definitely require another logger. 
 * Also, execution time seems to be an always-useful information to get.
 * <li>As a consequence, the method <code>sqlOccured(Spy, String, String)</code> is not implemented.
 * <li>Thanks to the use of <code>Marker</code>s, some options configured 
 * in the log4jdbc properties file can be set directly in the log4j2 configuration file: 
 * <ul>
 * <li>log4jdbc.dump.sql.select property can be set using the <code>Marker</code> <code>SELECT_MARKER</code>, 
 * named "LOG4JDBC_SELECT"
 * <li>log4jdbc.dump.sql.insert property can be set using the <code>Marker</code> <code>INSERT_MARKER</code>, 
 * named "LOG4JDBC_INSERT"
 * <li>log4jdbc.dump.sql.update property can be set using the <code>Marker</code> <code>UPDATE_MARKER</code>, 
 * named "LOG4JDBC_UPDATE"
 * <li>log4jdbc.dump.sql.delete property can be set using the <code>Marker</code> <code>DELETE_MARKER</code>, 
 * named "LOG4JDBC_DELETE"
 * <li>log4jdbc.dump.sql.create property can be set using the <code>Marker</code> <code>CREATE_MARKER</code>, 
 * named "LOG4JDBC_CREATE"
 * </ul>
 * These <code>Marker</code>s are all children of the <code>Marker</code> <code>SQL_MARKER</code>, named "LOG4JDBC_SQL".
 * These properties can also be set through the log4jdbc properties file. 
 * They would have priority over the <code>Marker</code>s.
 * <li>The interface <code>SpyLogDelegator</code>, 
 * and the classes <code>Slf4jSpyLogDelegator</code>, <code>DriverSpy</code>, 
 * <code>ConnectionSpy</code>, and <code>SpyLogFactory</code>, 
 * have been modified. See their corresponding javadoc for information about the changes.
 * </ul>
 *
 * @author Frederic Bastian
 * @see #LOGGER
 * @see #DEBUGLOGGER
 * @see net.sf.log4jdbc.Slf4jSpyLogDelegator
 * @see net.sf.log4jdbc.SpyLogDelegator
 * @see net.sf.log4jdbc.DriverSpy
 * @see net.sf.log4jdbc.SpyLogFactory
 */
public class Log4j2SpyLogDelegator implements SpyLogDelegator
{
	/**
	 * Logger responsible of logging all spy events
	 */
	private static final Logger LOGGER = LogManager.getLogger("log4jdbc.log4j2");
	/**
	 * Logger just for debugging things within log4jdbc itself (admin, setup, etc.)
	 * @see net.sf.log4jdbc.Slf4jSpyLogDelegator#debugLogger
	 */
	private static final Logger DEBUGLOGGER = LogManager.getLogger("log4jdbc.debug");
	
	/**
	 * <code>Marker</code> to log events generated by <code>Statement</code>s 
	 * (corresponds to the "jdbc.sqltiming" logger in the standard implementation)
	 */
	private static final Marker SQL_MARKER = MarkerManager.getMarker("LOG4JDBC_SQL");
	/**
	 * <code>Marker</code> to log events following <code>select</code> statements 
	 * (corresponds to the log4jdbc.dump.sql.select property)
	 */
	private static final Marker SELECT_MARKER = MarkerManager.getMarker("LOG4JDBC_SELECT", SQL_MARKER);
	/**
	 * <code>Marker</code> to log events following <code>insert</code> statements 
	 * (corresponds to the log4jdbc.dump.sql.insert property)
	 */
	private static final Marker INSERT_MARKER = MarkerManager.getMarker("LOG4JDBC_INSERT", SQL_MARKER);
	/**
	 * <code>Marker</code> to log events following <code>update</code> statements 
	 * (corresponds to the log4jdbc.dump.sql.update property)
	 */
	private static final Marker UPDATE_MARKER = MarkerManager.getMarker("LOG4JDBC_UPDATE", SQL_MARKER);
	/**
	 * <code>Marker</code> to log events following <code>delete</code> statements 
	 * (corresponds to the log4jdbc.dump.sql.delete property)
	 */
	private static final Marker DELETE_MARKER = MarkerManager.getMarker("LOG4JDBC_DELETE", SQL_MARKER);
	/**
	 * <code>Marker</code> to log events following <code>create</code> statements 
	 * (corresponds to the log4jdbc.dump.sql.create property)
	 */
	private static final Marker CREATE_MARKER = MarkerManager.getMarker("LOG4JDBC_CREATE", SQL_MARKER);
	/**
	 * <code>Marker</code> to log connections events
	 * (corresponds to the "jdbc.connection" logger in the standard implementation)
	 */
	private static final Marker CONNECTION_MARKER = MarkerManager.getMarker("LOG4JDBC_CONNECTION");
	/**
	 * <code>Marker</code> to log all JDBC calls including <code>ResultSet</code>s
	 */
	private static final Marker JDBC_MARKER = MarkerManager.getMarker("LOG4JDBC_JDBC");
	/**
	 * <code>Marker</code> to log all JDBC calls except for <code>ResultSet</code>s
	 * (corresponds to the "jdbc.audit" logger in the standard implementation)
	 */
	private static final Marker AUDIT_MARKER = MarkerManager.getMarker("LOG4JDBC_AUDIT", JDBC_MARKER);
	/**
	 * <code>Marker</code> to log <code>ResultSet</code>s calls
	 * (corresponds to the "jdbc.resultset" logger in the standard implementation)
	 */
	private static final Marker RESULTSET_MARKER = MarkerManager.getMarker("LOG4JDBC_RESULTSET", JDBC_MARKER);
	/**
     * <code>Marker</code> to log <code>Exception</code>s.
     * These are not specific to one logger in the standard implementation,
     * and they are not logged by the loggers "jdbc.resultset" and "jdbc.connection"
     * (bug, or a hope for no exception to occur for these events?)
     */
    private static final Marker EXCEPTION_MARKER = MarkerManager.getMarker("LOG4JDBC_EXCEPTION");

	public boolean isJdbcLoggingEnabled() {
		return LOGGER.isErrorEnabled();
	}

	public void exceptionOccured(Spy spy, String methodCall, Exception e,
			String sql, long execTime) {
		
		LOGGER.error(EXCEPTION_MARKER, new ExceptionOccuredMessage(spy, methodCall, 
	        sql, execTime, LOGGER.isDebugEnabled()), e);
	}

	public void methodReturned(Spy spy, String methodCall, String returnMsg) 
	{
		String classType = spy.getClassType();
	    Marker marker = ResultSetSpy.classTypeDescription.equals(classType)?
	        RESULTSET_MARKER:AUDIT_MARKER;
	    
	    LOGGER.info(marker, 
	    		new MethodReturnedMessage(spy, methodCall, returnMsg, LOGGER.isDebugEnabled(marker)));
	}

	@SuppressWarnings("unused")
	public void constructorReturned(Spy spy, String constructionInfo) {
        //not yet used in the current implementation of log4jdbc
	}

	@SuppressWarnings("unused")
	public void sqlOccurred(Spy spy, String methodCall, String sql) {
		//not implemented, 
		//as the features provided by the logger "jdbc.sqlonly" are not reproduced.
	}

	public void sqlTimingOccurred(Spy spy, long execTime, String methodCall,
			String sql) 
	{
		//test useless in the current implementation, 
		//as if error level is not enabled for this logger, 
		//the ConnectionSpy will not be used (see isjdbcLoggingEnabled())
		//might maybe change one day?
		/*if (!LOGGER.isErrorEnabled()) {
			return;
		}*/
		String operation = this.getSqlOperation(sql);
		if (DriverSpy.isDumpSqlFilteringOn() && !this.shouldSqlBeLogged(operation)) {
			return;
		}
		
		Marker marker = this.getStatementMarker(operation);
		SqlTimingOccurredMessage message = 
				new SqlTimingOccurredMessage(spy, execTime, methodCall, sql, LOGGER.isDebugEnabled());

		if (DriverSpy.isSqlTimingErrorThresholdEnabled() &&
				execTime >= DriverSpy.getSqlTimingErrorThresholdMsec()) {

			LOGGER.error(marker, message);

		} else if (LOGGER.isWarnEnabled()) {
			if (DriverSpy.isSqlTimingWarnThresholdEnabled() &&
					execTime >= DriverSpy.getSqlTimingWarnThresholdMsec()) {

				LOGGER.warn(marker, message);
			} else {
				LOGGER.info(marker, message);
			}
		}
	}

	/**
	 * Identify the operation performed by the <code>sql</code> statement 
	 * (either "select", "insert", "update", "delete", or "create").
	 *  
	 * @param sql 	A <code>String</code> representing the SQL statement to evaluate
	 * @return 		A <code>String</code> representing the operation 
	 * 				performed by the <code>sql</code> statement 
	 * 				(either "select", "insert", "update", "delete", or "create"), 
	 * 				or an empty <code>String</code> if the operation could not be determined 
	 * 				(<code>sql</code> <code>null</code>, etc.)
	 */
	private String getSqlOperation(String sql) 
	{
		if (sql == null) {
			return "";
		}
		sql = sql.trim();

		if (sql.length()<6) {
			return "";
		}
		return sql.substring(0,6).toLowerCase();
	}
	
	/**
	 * Return the appropriate <code>Marker</code> 
	 * (either <code>SQL_MARKER</code>, <code>SELECT_MARKER</code>, 
	 * <code>INSERT_MARKER</code>, <code>UPDATE_MARKER</code>, <code>DELETE_MARKER</code>, 
	 * or <code>CREATE_MARKER</code>) depending on the <code>operation</code> 
	 * performed by a SQL statement 
	 * (either "select", "insert", "update", "delete", or "create").
	 * @param operation 	a <code>String</code> representing the <code>operation</code> 
	 * 						performed by a SQL statement 
	 * 						(either "select", "insert", "update", "delete", or "create"). 
	 * @return 				the appropriate <code>Marker</code> depending on the <code>operation</code>: 
	 * 						<code>SELECT_MARKER</code> if <code>operation</code> is equal to "select".
	 * 						<code>INSERT_MARKER</code> if <code>operation</code> is equal to "insert".
	 * 						<code>UPDATE_MARKER</code> if <code>operation</code> is equal to "update".
	 * 						<code>DELETE_MARKER</code> if <code>operation</code> is equal to "delete".
	 * 						<code>CREATE_MARKER</code> if <code>operation</code> is equal to "create".
	 * 						<code>SQL_MARKER</code> otherwise.
	 * @see #SQL_MARKER
	 * @see #SELECT_MARKER
	 * @see #INSERT_MARKER
	 * @see #UPDATE_MARKER
	 * @see #DELETE_MARKER
	 * @see #CREATE_MARKER
	 */
	private Marker getStatementMarker(String operation)
	{
		if (operation == null) {
			return SQL_MARKER;
		} else if ("select".equals(operation)) {
			return SELECT_MARKER;
		} else if ("insert".equals(operation)) {
			return INSERT_MARKER;
		} else if ("update".equals(operation)) {
			return UPDATE_MARKER;
		} else if ("delete".equals(operation)) {
			return DELETE_MARKER;
		} else if ("create".equals(operation)) {
			return CREATE_MARKER;
		}
		return SQL_MARKER;
	}
	
	/**
	   * Determine if the given <code>operation</code> of an SQL statement 
	   * should be logged or not
	   * based on the various DumpSqlXXXXXX flags.
	   *
	   * @param operation	A <code>String</code> representing the operation of a SQL statement 
	   * 					(either "select", "insert", "update", "delete", or "create").
	   * @return 	<code>true</code> if the SQL statement 
	   * 			executing the given <code>operation</code> should be logged, false if not.
	   */
	  private boolean shouldSqlBeLogged(String operation)
	  {
	    return 
	      (operation == null) ||
	      (DriverSpy.isDumpSqlSelect() && "select".equals(operation)) ||
	      (DriverSpy.isDumpSqlInsert() && "insert".equals(operation)) ||
	      (DriverSpy.isDumpSqlUpdate() && "update".equals(operation)) ||
	      (DriverSpy.isDumpSqlDelete() && "delete".equals(operation)) ||
	      (DriverSpy.isDumpSqlCreate() && "create".equals(operation));
	  }

	public void connectionOpened(Spy spy, long execTime) 
	{
		this.connectionOpenedOrClosed(spy, execTime, ConnectionMessage.OPENING);
	}

	public void connectionClosed(Spy spy, long execTime) 
	{
		this.connectionOpenedOrClosed(spy, execTime, ConnectionMessage.CLOSING);
	}
	
	/**
	 * 
	 * @param spy 			<code>ConnectionSpy</code> that was opened or closed.
	 * @param execTime 		A <code>long</code> defining the time elapsed to open or close the connection in ms
	 * 						Caller should pass -1 if not used
	 * @param operation 	an <code>int</code> to define if the operation was to open, or to close connection. 
	 * 						Should be equals to <code>ConnectionMessage.OPENING</code> 
	 * 						if the operation was to open the connection, 
	 * 						to <code>ConnectionMessage.CLOSING</code> if the operation was to close the connection.
	 */
	private void connectionOpenedOrClosed(Spy spy, long execTime, int operation)
	{
		LOGGER.info(CONNECTION_MARKER, 
				    new ConnectionMessage(spy, execTime, operation, LOGGER.isDebugEnabled()));
	}

	public void debug(String msg) 
	{
		DEBUGLOGGER.debug(msg);
	}

}
