package com.jpaulmorrison.fbp.core.components.jdbc;

import java.lang.reflect.Field;

import java.sql.*;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpaulmorrison.fbp.core.engine.Component; // Using 'Connection', 'Statement' and 'ResultSet' classes in java.sql package
import com.jpaulmorrison.fbp.core.engine.ComponentDescription;
import com.jpaulmorrison.fbp.core.engine.InPort;
import com.jpaulmorrison.fbp.core.engine.InPorts;
import com.jpaulmorrison.fbp.core.engine.InputPort;
import com.jpaulmorrison.fbp.core.engine.OutPort;
import com.jpaulmorrison.fbp.core.engine.OutputPort;
import com.jpaulmorrison.fbp.core.engine.Packet;

@ComponentDescription("Write incoming IPs to MySQL table")
@InPorts({ @InPort(value = "DATABASE", description = "Database name", type = String.class, isIIP = true),
		@InPort(value = "USER", description = "User name", type = String.class, isIIP = true),
		@InPort(value = "CLASS", description = "Object class", type = String.class, isIIP = true),
		@InPort(value = "FIELDS", description = "Field correspondences", type = String.class, isIIP = true),
		@InPort(value = "PSWD", description = "Password obtained from file", type = String.class, isIIP = true),
		@InPort("IN") })
@OutPort(value = "OUT", description = "Table rows", optional = true)

public class WriteJDBC extends Component {

	// adapted from
	// https://www.ntu.edu.sg/home/ehchua/programming/java/JDBC_Basic.html

	// private OutputPort outPort;

	private InputPort pswdPort;
	private InputPort dBNPort;
	private InputPort userPort;
	private InputPort classPort;
	private InputPort fldsPort;
	private InputPort inPort;
	private OutputPort outPort;

	@Override
	protected void execute() throws Exception {

		Packet<?> pp = pswdPort.receive();

		String pswd = (String) pp.getContent();
		drop(pp);
		pswdPort.close();

		pp = dBNPort.receive();

		String dbTable = (String) pp.getContent();
		drop(pp);
		dBNPort.close();

		pp = userPort.receive();

		String user = (String) pp.getContent();
		drop(pp);
		userPort.close();

		pp = classPort.receive();

		String objClass = (String) pp.getContent();
		drop(pp);
		classPort.close();

		// Class<?> curClass = cls;
		Class<?> curClass = Class.forName(objClass);

		String[] iipContents = dbTable.split("!", 2);

		pp = fldsPort.receive();

		String fldsStr = (String) pp.getContent();
		drop(pp);
		fldsPort.close();

		ObjectMapper mapper = new ObjectMapper();
		FieldInfo[] fiArray = null;
		try {

			fiArray = mapper.readerForListOf(FieldInfo[].class).readValue(fldsStr);

		} catch (Exception e) {
			System.out.println("Error parsing JSON string");
			return;
		}

		try (

				// Step 1: Allocate a database 'Connection' object
				Connection conn = DriverManager.getConnection(
						// "jdbc:mysql://localhost:3306/ebookshop?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC",
						// "root", pswd); // For MySQL only

						iipContents[0] + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC", user, pswd);

				Statement stmt = conn.createStatement();) {

			String strSelect = "select * from " + iipContents[1];
			ResultSet rs = stmt.executeQuery(strSelect);
			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();

			// String strSelect = "select * from " + iipContents[1];
			// System.out.println("The SQL statement is: \"" + strSelect + "\"\n"); // Echo
			String strDelete = "delete from " + iipContents[1]; // no qualifier
			System.out.println("The SQL statement is: \"" + strDelete + "\"\n"); // Echo

			int countDeleted = stmt.executeUpdate(strDelete);
			System.out.println(countDeleted + " records deleted.\n");

			HashMap<String, String> hmColumns = new HashMap<String, String>();

			try {

				for (int i = 1; i <= numberOfColumns; i++) {
					hmColumns.put(rsmd.getColumnName(i), rsmd.getColumnTypeName(i));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// number of columns should match number of fields in curClass - so
			// let's test...

			int n = curClass.getFields().length;
			if (n != numberOfColumns) {
				System.out.println(
						"Number of class fields - " + n + " does not match number of columns - " + numberOfColumns);
				return;
			}

			HashMap<String, Class<?>> hmFields = new HashMap<String, Class<?>>();

			for (Field fd : curClass.getFields()) {
				hmFields.put(fd.getName(), fd.getType());
			}

			// receive and process IPs

			int rowCount = 0;
			Packet<?> pIn;
			while ((pIn = inPort.receive()) != null) {

				Object o = pIn.getContent();
				if (o.getClass() != curClass) {
					System.out.println("Unexpected class in incoming IP: " + o.getClass());
					continue;
				}
				String sqlInsert = "insert into " + iipContents[1] + "(";
				String sqlValues = " values(";
				// iterate through hmColumns
				String cma = "";
				for (String col : hmColumns.keySet()) {
					Field field = null;
					for (int i = 0; i < fiArray.length; i++) {
						// String colName = fiArray[i].colName;
						String objField = fiArray[i].objField;
						if (fiArray[i].colName.equals(col)) {
							field = curClass.getDeclaredField(objField);
							break;
						}
					}

					if (field == null) {
						System.out.println("Table column \"" + col + "\" not found in Field Info:" + fldsStr);
						return;
					}

					sqlInsert += cma + col;
					Object o2 = field.get(o);
					sqlValues += cma + "\"" + o2 /* .toString() */ + "\"";
					cma = ",";
				}
				sqlInsert += ")";
				sqlValues += ")";
				// String sqlInsert = "insert into sales values (3001, 'Gone Fishing', 'Kumar',
				// 'CAD11.11', 11)";
				System.out.println("The SQL statement is: " + sqlInsert + " " + sqlValues + "\n"); // Echo for debugging
				int countInserted = stmt.executeUpdate(sqlInsert + " " + sqlValues);
				if (countInserted != 1) {
					System.out.println("Couldn't insert record:\n");
					System.out.println("... " + sqlInsert);
				}

				if (outPort.isConnected()) {
					outPort.send(pIn);
				} else {
					drop(pIn);
				}
			}

			System.out.println("Total number of records = " + rowCount);
			// outPort.send(create("Total number of records = " + rowCount));

		} catch (SQLException ex) {
			// System.out.println("SQL Exception");
			ex.printStackTrace();
		} /*
			 * catch (ClassNotFoundException ex) {
			 * //System.out.println("Class Not Found Exception"); ex.printStackTrace(); }
			 * catch (InvocationTargetException ex) {
			 * //System.out.println("Class Not Found Exception"); ex.printStackTrace(); }
			 */
		// Step 5: Close conn and stmt - Done automatically by
		// try-with-resources (JDK 7)
	}

	@Override
	protected void openPorts() {
		pswdPort = openInput("PSWD");
		userPort = openInput("USER");
		dBNPort = openInput("DATABASE");
		classPort = openInput("CLASS");
		fldsPort = openInput("FIELDS");
		inPort = openInput("IN");
		outPort = openOutput("OUT");
	}

	public class FieldInfo {
		String colName;
		String objField;
	}
}