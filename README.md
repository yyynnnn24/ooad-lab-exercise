# FCI Seminar Management System

===================================================================
                   ⚠️ HOW TO RUN THE PROJECT ⚠️
===================================================================

There are two ways to run Java programs, depending on whether we are 
using the Database or not.

-------------------------------------------------------------------
SCENARIO 1: No Database
-------------------------------------------------------------------
If you are running a version of the code that does NOT connect to SQLite:

You can run it directly by clicking the run button.

-------------------------------------------------------------------
SCENARIO 2: With Database Connection
-------------------------------------------------------------------
You CANNOT run it directly. You must link the JAR file in the command.

1. Make sure `sqlite-jdbc-3.46.0.0.jar` is in this folder.
2. Run these commands:

   [COMPILE]
   javac -cp ".;sqlite-jdbc-3.46.0.0.jar" *.java

   [RUN]
   java -cp ".;sqlite-jdbc-3.46.0.0.jar" LoginScreen

*(Note: If you get "No suitable driver found", you are using the Scenario 1 
command by mistake. Please use the Scenario 2 command above.)*

===================================================================
                   🔑 LOGIN CREDENTIALS
===================================================================
For testing:

1. STUDENT ACCOUNT
   User ID:  s001
   Password: 123
   Role:     Student

2. STAFF ACCOUNT
   User ID:  a001
   Password: 123
   Role:     Staff

The database logic is located in DatabaseHandler.java. This file allows you to create tables and insert data.
If you want to view the data in database click this link: http://inloop.github.io/sqlite-viewer/ and drop seminar_system.db
