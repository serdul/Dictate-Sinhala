## 2024-04-04 - Fix SQL Injection via String Concatenation in SQLiteHelpers
**Vulnerability:** SQL Injection in UsageDatabaseHelper.java and PromptsDatabaseHelper.java
**Learning:** Database helpers previously used direct string concatenation for SQL queries, exposing the application to SQL injection attacks and crash vectors if input strings contained quotes.
**Prevention:** Always use parameterized queries (e.g., WHERE MODEL_NAME=? and new String[]{model}) for SQLite database interactions instead of string interpolation.
